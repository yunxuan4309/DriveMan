package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.FeeStandard;
import com.homework.driveman.entity.LicenseConfig;
import com.homework.driveman.entity.LicenseUpgrade;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.LicenseConfigMapper;
import com.homework.driveman.mapper.LicenseUpgradeMapper;
import com.homework.driveman.service.IDisabilityInfoService;
import com.homework.driveman.service.IFeeStandardService;
import com.homework.driveman.service.ILicenseUpgradeService;
import com.homework.driveman.service.IPaymentRecordService;
import com.homework.driveman.service.IPhysicalExamService;
import com.homework.driveman.service.ISpecialPersonRecordService;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.web.ServiceCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 增驾申请服务实现
 * 依据：《机动车驾驶证申领和使用规定》（公安部令第172号，2025年1月1日起施行）
 */
@Slf4j
@Service
public class LicenseUpgradeServiceImpl extends ServiceImpl<LicenseUpgradeMapper, LicenseUpgrade> implements ILicenseUpgradeService {

    @Autowired
    private IUserService userService;

    @Autowired
    private IPhysicalExamService physicalExamService;

    @Autowired
    private IFeeStandardService feeStandardService;

    @Autowired
    private IPaymentRecordService paymentRecordService;

    @Autowired
    private com.homework.driveman.mapper.PaymentRecordMapper paymentRecordMapper;

    @Autowired
    private LicenseConfigMapper licenseConfigMapper;

    @Autowired
    private IDisabilityInfoService disabilityInfoService;

    @Autowired
    private ISpecialPersonRecordService specialPersonRecordService;

    // ==================== 车型等级定义 ====================

    /** C1/C2/C5/C6 同属小型汽车类 */
    private static final Set<String> GROUP_C = new HashSet<>(Arrays.asList("C1", "C2", "C5", "C6"));
    /** B1/B2 同属中型/大型货车类 */
    private static final Set<String> GROUP_B = new HashSet<>(Arrays.asList("B1", "B2"));
    /** A1/A3 同属大型客车/城市公交类 */
    private static final Set<String> GROUP_A_BUS = new HashSet<>(Arrays.asList("A1", "A3"));
    /** D/E/F 摩托车类 */
    private static final Set<String> GROUP_MOTO = new HashSet<>(Arrays.asList("D", "E", "F"));
    /** M/N/P 特种车辆类 */
    private static final Set<String> GROUP_SPECIAL = new HashSet<>(Arrays.asList("M", "N", "P"));

    // ==================== 年龄要求（2025年新规） ====================

    /** 各车型最低年龄要求 */
    private static final java.util.Map<String, Integer> MIN_AGE = new java.util.HashMap<>();
    static {
        MIN_AGE.put("C1", 18); MIN_AGE.put("C2", 18); MIN_AGE.put("C5", 18); MIN_AGE.put("C6", 20);
        MIN_AGE.put("B1", 20); MIN_AGE.put("B2", 20);
        MIN_AGE.put("A1", 22); MIN_AGE.put("A2", 22); MIN_AGE.put("A3", 20);
        MIN_AGE.put("D", 18); MIN_AGE.put("E", 18); MIN_AGE.put("F", 18);
        MIN_AGE.put("M", 18); MIN_AGE.put("N", 20); MIN_AGE.put("P", 20);
    }

    // ==================== 升级路径与年限要求 ====================

    /**
     * 升级路径定义：key=目标车型, value=允许的源车型及所需持有年限(年)
     */
    private static final java.util.Map<String, List<UpgradePath>> UPGRADE_RULES = new java.util.HashMap<>();
    static {
        // C6: 需持有C1/C2一年以上
        UPGRADE_RULES.put("C6", Arrays.asList(
                new UpgradePath("C1", 1), new UpgradePath("C2", 1)
        ));
        // B1: 需持有A3/B2/C1/C2/C3/C4二年以上
        UPGRADE_RULES.put("B1", Arrays.asList(
                new UpgradePath("A3", 2), new UpgradePath("B2", 2),
                new UpgradePath("C1", 2), new UpgradePath("C2", 2),
                new UpgradePath("C3", 2), new UpgradePath("C4", 2)
        ));
        // A2: 需持有B1/B2二年以上，或A1一年以上
        UPGRADE_RULES.put("A2", Arrays.asList(
                new UpgradePath("B1", 2), new UpgradePath("B2", 2),
                new UpgradePath("A1", 1)
        ));
        // A1: 需持有A3/B1二年以上，或B2三年以上，或A2一年以上
        UPGRADE_RULES.put("A1", Arrays.asList(
                new UpgradePath("A3", 2), new UpgradePath("B1", 2),
                new UpgradePath("B2", 3), new UpgradePath("A2", 1)
        ));
        // A3: 可直接申请（初次申领），也可增驾
        UPGRADE_RULES.put("A3", Arrays.asList(
                new UpgradePath("C1", 1), new UpgradePath("C2", 1)
        ));
        // B2: 可直接申请（初次申领），也可增驾
        UPGRADE_RULES.put("B2", Arrays.asList(
                new UpgradePath("C1", 1), new UpgradePath("C2", 1)
        ));
        // D/E: 摩托车升级
        UPGRADE_RULES.put("D", Arrays.asList(
                new UpgradePath("E", 1), new UpgradePath("F", 1)
        ));
        UPGRADE_RULES.put("E", Arrays.asList(
                new UpgradePath("F", 1)
        ));
    }

    // ==================== 准驾覆盖关系（原车型已覆盖目标车型时拒绝增驾） ====================

    private static final java.util.Map<String, Set<String>> COVERAGE_MAP = new java.util.HashMap<>();
    static {
        COVERAGE_MAP.put("C1", new HashSet<>(Arrays.asList("C1", "C2")));
        COVERAGE_MAP.put("C2", new HashSet<>(Arrays.asList("C2")));
        COVERAGE_MAP.put("D", new HashSet<>(Arrays.asList("D", "E", "F")));
        COVERAGE_MAP.put("E", new HashSet<>(Arrays.asList("E", "F")));
        COVERAGE_MAP.put("F", new HashSet<>(Arrays.asList("F")));
        // A/B 级驾驶证默认覆盖同类及以下车型
        COVERAGE_MAP.put("A1", new HashSet<>(Arrays.asList("A1", "A3", "B1", "B2", "C1", "C2", "C5", "C6", "M", "N", "P")));
        COVERAGE_MAP.put("A3", new HashSet<>(Arrays.asList("A3", "C1", "C2", "C5", "C6")));
        COVERAGE_MAP.put("B1", new HashSet<>(Arrays.asList("B1", "C1", "C2", "C5", "C6", "M")));
        COVERAGE_MAP.put("B2", new HashSet<>(Arrays.asList("B2", "C1", "C2", "C5", "C6", "M")));
        COVERAGE_MAP.put("C5", new HashSet<>(Arrays.asList("C5")));
        COVERAGE_MAP.put("C6", new HashSet<>(Arrays.asList("C6")));
        COVERAGE_MAP.put("M", new HashSet<>(Arrays.asList("M")));
        COVERAGE_MAP.put("N", new HashSet<>(Arrays.asList("N")));
        COVERAGE_MAP.put("P", new HashSet<>(Arrays.asList("P")));
    }

    /** 升级路径内部类 */
    private static class UpgradePath {
        String source;
        int years;
        UpgradePath(String source, int years) { this.source = source; this.years = years; }
    }

    // ==================== 业务方法 ====================

    @Override
    public LicenseUpgrade apply(Integer studentId, String targetLicense, Integer upgradeType, String licenseFileId, boolean skipAgeCheck) {
        User student = userService.getById(studentId);
        if (student == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "学员不存在");
        }
        if (student.getLicenseType() == null) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "您尚未报名，无法申请增驾");
        }

        String originalLicense = student.getLicenseType();

        // 检查是否已有进行中的申请（已完结的不拦截）
        Long count = lambdaQuery()
                .eq(LicenseUpgrade::getStudentId, studentId)
                .in(LicenseUpgrade::getStatus, 0, 1)
                .and(w -> w.ne(LicenseUpgrade::getExamStatus, 1)
                        .or().isNull(LicenseUpgrade::getExamStatus))
                .count();
        if (count > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "您已有进行中的增驾申请");
        }

        // 校验目标车型合法性
        if (!MIN_AGE.containsKey(targetLicense)) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "无效的目标车型: " + targetLicense);
        }

        // 目标车型与当前车型相同 → 无需增驾
        if (originalLicense.equals(targetLicense)) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                    "您已持有 " + targetLicense + " 车型，无需增驾");
        }

        // 准驾覆盖校验：原车型已覆盖目标车型时拒绝
        Set<String> covered = COVERAGE_MAP.get(originalLicense);
        if (covered != null && covered.contains(targetLicense)) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                    originalLicense + " 已准驾 " + targetLicense + "，无需增驾");
        }

        // 校验增驾类型并执行对应校验
        if (upgradeType == 1) {
            // 同级增驾
            if (!isSameLevel(originalLicense, targetLicense)) {
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                        originalLicense + " 与 " + targetLicense + " 不是同级车型，不能申请同级增驾");
            }
        } else if (upgradeType == 2) {
            // 升级增驾：校验年龄、车型路径，驾驶证材料由管理员审核时校验
            // 如果当前驾照不满足升级路径，通过已完成的增驾记录追溯原始驾照
            // （如 C1→C6 后用户持有 C6，但 C6 不是 B1 的允许来源，应追溯为 C1）
            String effectiveOriginal = originalLicense;
            boolean tracedBack = false;
            List<UpgradePath> candidatePaths = UPGRADE_RULES.get(targetLicense);
            if (candidatePaths != null) {
                boolean directMatch = false;
                for (UpgradePath p : candidatePaths) {
                    if (p.source.equals(originalLicense)) { directMatch = true; break; }
                }
                if (!directMatch) {
                    String resolved = resolveOriginalLicense(studentId, originalLicense, targetLicense);
                    if (!resolved.equals(originalLicense)) {
                        effectiveOriginal = resolved;
                        tracedBack = true;
                    }
                }
            }
            if (!skipAgeCheck) {
                validateUpgradeBasicConditions(student, effectiveOriginal, targetLicense, tracedBack);
            }
            if (licenseFileId == null || licenseFileId.isEmpty()) {
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "升级增驾需上传驾驶证材料");
            }
        } else {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "无效的增驾类型");
        }

        LicenseUpgrade upgrade = new LicenseUpgrade();
        upgrade.setStudentId(studentId);
        upgrade.setOriginalLicense(originalLicense);
        upgrade.setTargetLicense(targetLicense);
        upgrade.setUpgradeType(upgradeType);
        upgrade.setLicenseFileId(licenseFileId);
        upgrade.setStatus(0); // 待审核
        upgrade.setExamStatus(0); // 待考试
        save(upgrade);

        log.info("增驾申请提交成功: studentId={}, {} -> {}, type={}, licenseFileId={}",
                studentId, originalLicense, targetLicense, upgradeType, licenseFileId);
        return upgrade;
    }

    @Override
    public List<LicenseUpgrade> listByStudent(Integer studentId) {
        return lambdaQuery()
                .eq(LicenseUpgrade::getStudentId, studentId)
                .orderByDesc(LicenseUpgrade::getCreateTime)
                .list();
    }

    @Override
    public Page<LicenseUpgrade> pageMyUpgrades(Page<LicenseUpgrade> page, Integer studentId,
                                                Integer status, String targetLicense) {
        return lambdaQuery()
                .eq(LicenseUpgrade::getStudentId, studentId)
                .eq(status != null, LicenseUpgrade::getStatus, status)
                .eq(targetLicense != null && !targetLicense.isEmpty(), LicenseUpgrade::getTargetLicense, targetLicense)
                .orderByDesc(LicenseUpgrade::getCreateTime)
                .page(page);
    }

    @Override
    public void audit(Integer id, Integer status, String remark, String skipSubjects) {
        LicenseUpgrade upgrade = getById(id);
        if (upgrade == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "增驾申请不存在");
        }
        if (upgrade.getStatus() != 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该申请已处理，无法重复审核");
        }

        // 校验跳过的科目
        Set<Integer> skipSet = new HashSet<>();
        if (status == 1 && skipSubjects != null && !skipSubjects.isEmpty()) {
            for (String s : skipSubjects.split(",")) {
                try {
                    int subj = Integer.parseInt(s.trim());
                    if (subj < 1 || subj > 4) {
                        throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                                "无效的科目编号: " + subj + "，有效范围 1-4");
                    }
                    skipSet.add(subj);
                } catch (NumberFormatException e) {
                    throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                            "无效的科目编号格式: " + s);
                }
            }
            // 不能跳过全部科目
            if (skipSet.size() >= 4) {
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                        "不能跳过全部科目，至少保留一科进行考试");
            }
        }

        // 升级增驾（upgradeType=2）审核通过时，要求学员已完成目标车型的体检
        if (status == 1 && upgrade.getUpgradeType() == 2) {
            physicalExamService.checkPassedForLicense(upgrade.getStudentId(), upgrade.getTargetLicense());
        }

        // C5 目标车型审核通过时，校验残疾信息已通过 + 不在禁驾期
        if (status == 1 && "C5".equals(upgrade.getTargetLicense())) {
            if (!disabilityInfoService.isAuditPassed(upgrade.getStudentId())) {
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                        "目标车型为 C5，但该学员未通过残疾信息审核，请先审核其残疾信息");
            }
            if (specialPersonRecordService.isInBanPeriod(upgrade.getStudentId())) {
                LocalDate banEnd = specialPersonRecordService.getBanEndDate(upgrade.getStudentId());
                String banMsg;
                if (banEnd != null && banEnd.equals(LocalDate.MAX)) {
                    banMsg = "该学员处于终生禁驾期，无法完成增驾";
                } else {
                    banMsg = "该学员处于禁驾期（截止至 " + banEnd + "），无法完成增驾";
                }
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, banMsg);
            }
        }

        upgrade.setStatus(status);
        upgrade.setRemark(remark);
        upgrade.setSkipSubjects(skipSubjects);

        updateById(upgrade);

        if (status == 1) {
            // 审核通过时，自动生成增驾费账单
            FeeStandard pkg = feeStandardService.lambdaQuery()
                    .eq(FeeStandard::getLicenseType, upgrade.getTargetLicense())
                    .isNull(FeeStandard::getSubject)
                    .orderByDesc(FeeStandard::getAmount)
                    .last("LIMIT 1")
                    .one();
            if (pkg != null) {
                paymentRecordService.autoCreate(upgrade.getStudentId(), "upgrade_fee",
                        upgrade.getId(), pkg.getAmount(),
                        upgrade.getTargetLicense() + " 增驾套餐 " + pkg.getDescription());
                log.info("增驾套餐账单已生成: studentId={}, targetLicense={}, amount={}",
                        upgrade.getStudentId(), upgrade.getTargetLicense(), pkg.getAmount());
            } else {
                // 没有配置套餐时，生成一个默认金额的账单（管理员可在费用标准页面配置）
                paymentRecordService.autoCreate(upgrade.getStudentId(), "upgrade_fee",
                        upgrade.getId(), new java.math.BigDecimal("2000.00"),
                        upgrade.getTargetLicense() + " 增驾费（默认价格，管理端可修改）");
                log.warn("未找到 {} 的套餐费用标准，已使用默认金额 2000 元生成账单，" +
                        "请管理员在费用标准页面补充配置", upgrade.getTargetLicense());
            }

            String msg = skipSet.isEmpty() ? "待考试"
                    : "跳过科目 " + skipSubjects + "，其余科目待考试";
            log.info("增驾申请审核通过，{}: id={}, studentId={}, targetLicense={}",
                    msg, id, upgrade.getStudentId(), upgrade.getTargetLicense());
        }

        log.info("增驾申请审核完成: id={}, status={}", id, status);
    }

    @Override
    public Map<String, Object> getProgress(Integer id) {
        LicenseUpgrade upgrade = getById(id);
        if (upgrade == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "增驾申请不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("upgrade", upgrade);

        // 1. 查询支付状态 — 该增驾的 upgrade_fee 是否已支付
        LambdaQueryWrapper<com.homework.driveman.entity.PaymentRecord> paidQw = Wrappers.lambdaQuery();
        paidQw.eq(com.homework.driveman.entity.PaymentRecord::getStudentId, upgrade.getStudentId())
              .eq(com.homework.driveman.entity.PaymentRecord::getBizType, "upgrade_fee")
              .eq(com.homework.driveman.entity.PaymentRecord::getBizId, upgrade.getId())
              .eq(com.homework.driveman.entity.PaymentRecord::getStatus, 1);
        long paidCount = paymentRecordMapper.selectCount(paidQw);
        result.put("paid", paidCount > 0);

        // 2. 解析免考科目
        Set<Integer> skippedSet = new HashSet<>();
        if (upgrade.getSkipSubjects() != null && !upgrade.getSkipSubjects().isEmpty()) {
            for (String s : upgrade.getSkipSubjects().split(",")) {
                skippedSet.add(Integer.parseInt(s.trim()));
            }
        }
        result.put("skippedSubjects", skippedSet);

        // 3. 查询已通过的考试科目（通过 exam_registration → exam_session 关联）
        List<Integer> passedSubjects = baseMapper.selectPassedSubjects(
                upgrade.getStudentId(), upgrade.getTargetLicense());
        result.put("passedSubjects", passedSubjects);

        // 4. 计算待考科目（只检查目标车型实际配置的科目）
        Set<Integer> targetSubjects = licenseConfigMapper.selectList(
                new LambdaQueryWrapper<LicenseConfig>()
                        .eq(LicenseConfig::getLicenseType, upgrade.getTargetLicense())
                        .select(LicenseConfig::getSubject))
                .stream().map(LicenseConfig::getSubject).collect(Collectors.toSet());
        List<Integer> pendingSubjects = new ArrayList<>();
        for (Integer subject : targetSubjects) {
            if (!skippedSet.contains(subject) && !passedSubjects.contains(subject)) {
                pendingSubjects.add(subject);
            }
        }
        result.put("pendingSubjects", pendingSubjects);
        result.put("allPassed", pendingSubjects.isEmpty());

        return result;
    }

    @Override
    public void completeUpgrade(Integer id) {
        LicenseUpgrade upgrade = getById(id);
        if (upgrade == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "增驾申请不存在");
        }
        if (upgrade.getStatus() != 1) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该申请未通过审核，无法完成增驾");
        }
        if (upgrade.getExamStatus() != null && upgrade.getExamStatus() == 1) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该增驾已完成，无需重复操作");
        }

        // 1. 校验增驾费已支付
        LambdaQueryWrapper<com.homework.driveman.entity.PaymentRecord> allQw = Wrappers.lambdaQuery();
        allQw.eq(com.homework.driveman.entity.PaymentRecord::getStudentId, upgrade.getStudentId())
             .eq(com.homework.driveman.entity.PaymentRecord::getBizType, "upgrade_fee")
             .eq(com.homework.driveman.entity.PaymentRecord::getBizId, upgrade.getId());
        long unpaidCount = paymentRecordMapper.selectCount(allQw);

        LambdaQueryWrapper<com.homework.driveman.entity.PaymentRecord> paidOnlyQw = Wrappers.lambdaQuery();
        paidOnlyQw.eq(com.homework.driveman.entity.PaymentRecord::getStudentId, upgrade.getStudentId())
                  .eq(com.homework.driveman.entity.PaymentRecord::getBizType, "upgrade_fee")
                  .eq(com.homework.driveman.entity.PaymentRecord::getBizId, upgrade.getId())
                  .eq(com.homework.driveman.entity.PaymentRecord::getStatus, 1);
        long paidCount = paymentRecordMapper.selectCount(paidOnlyQw);
        if (paidCount == 0 && unpaidCount > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT,
                    "学员尚未支付增驾费，请先完成缴费");
        }

        // 2. 查询目标车型实际配置的科目（只检查这些科目）
        Set<Integer> skippedSet = new HashSet<>();
        if (upgrade.getSkipSubjects() != null && !upgrade.getSkipSubjects().isEmpty()) {
            for (String s : upgrade.getSkipSubjects().split(",")) {
                skippedSet.add(Integer.parseInt(s.trim()));
            }
        }

        List<LicenseConfig> targetConfigs = licenseConfigMapper.selectList(
                new LambdaQueryWrapper<LicenseConfig>()
                        .eq(LicenseConfig::getLicenseType, upgrade.getTargetLicense()));
        Set<Integer> targetSubjects = targetConfigs.stream()
                .map(LicenseConfig::getSubject).collect(Collectors.toSet());

        List<Integer> passedSubjects = baseMapper.selectPassedSubjects(
                upgrade.getStudentId(), upgrade.getTargetLicense());

        List<Integer> pendingSubjects = new ArrayList<>();
        for (Integer subject : targetSubjects) {
            if (!skippedSet.contains(subject) && !passedSubjects.contains(subject)) {
                pendingSubjects.add(subject);
            }
        }

        if (!pendingSubjects.isEmpty()) {
            String pendingStr = pendingSubjects.stream()
                    .map(s -> "科目" + s).collect(Collectors.joining("、"));
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                    "以下科目尚未通过考试：" + pendingStr);
        }

        // 3. 全部条件满足 — 完成增驾
        upgrade.setExamStatus(1);
        upgrade.setExamRemark("完成增驾");
        updateById(upgrade);

        User student = userService.getById(upgrade.getStudentId());
        if (student != null) {
            student.setLicenseType(upgrade.getTargetLicense());
            student.setLicenseObtainedDate(LocalDateTime.now());
            userService.updateById(student);
            log.info("增驾完成，学员车型已更新: userId={}, {} -> {}, licenseObtainedDate={}",
                    student.getUserId(), upgrade.getOriginalLicense(), upgrade.getTargetLicense(),
                    student.getLicenseObtainedDate());
        }
    }

    @Override
    public Page<Map<String, Object>> pageSearch(Page<?> page, String keyword, String originalLicense,
                                                 String targetLicense, Integer status, Integer examStatus,
                                                 LocalDateTime createTimeStart, LocalDateTime createTimeEnd) {
        return baseMapper.selectPageWithDetails(page, keyword, originalLicense,
                targetLicense, status, examStatus, createTimeStart, createTimeEnd);
    }

    // ==================== 校验方法 ====================

    /**
     * 追溯原始驾照：通过已完成的增驾记录链式查找原始驾照类型
     * 例如 C1→C6 完成后当前为 C6，申请 B1 时 C6 不满足路径，追溯返回 C1
     */
    private String resolveOriginalLicense(Integer studentId, String currentLicense, String targetLicense) {
        List<UpgradePath> paths = UPGRADE_RULES.get(targetLicense);
        if (paths == null) return currentLicense;

        for (UpgradePath p : paths) {
            if (p.source.equals(currentLicense)) return currentLicense;
        }

        LicenseUpgrade completed = lambdaQuery()
                .eq(LicenseUpgrade::getStudentId, studentId)
                .eq(LicenseUpgrade::getTargetLicense, currentLicense)
                .eq(LicenseUpgrade::getStatus, 1)
                .eq(LicenseUpgrade::getExamStatus, 1)
                .orderByDesc(LicenseUpgrade::getCreateTime)
                .last("LIMIT 1")
                .one();

        if (completed != null) {
            return resolveOriginalLicense(studentId, completed.getOriginalLicense(), targetLicense);
        }

        return currentLicense;
    }

    /**
     * 升级增驾基础校验（提交申请时执行）
     * 持有年限由管理员通过审核驾驶证材料来判断
     */
    private void validateUpgradeBasicConditions(User student, String original, String target, boolean tracedBack) {
        // 1. 校验升级路径是否合法
        List<UpgradePath> paths = UPGRADE_RULES.get(target);
        if (paths == null) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                    "暂不支持增驾到 " + target + " 车型");
        }

        UpgradePath matchedPath = null;
        for (UpgradePath path : paths) {
            if (path.source.equals(original)) {
                matchedPath = path;
                break;
            }
        }
        if (matchedPath == null) {
            StringBuilder allowed = new StringBuilder();
            for (UpgradePath p : paths) allowed.append(p.source).append("/");
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                    original + " 不能升级到 " + target + "，允许的来源车型: "
                            + allowed.substring(0, allowed.length() - 1));
        }

        // 2. 校验年龄
        if (student.getIdCard() == null || student.getIdCard().length() != 18) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                    "身份证号信息不完整，无法校验年龄");
        }
        int age = calculateAgeFromIdCard(student.getIdCard());
        Integer minAge = MIN_AGE.get(target);
        if (minAge != null && age < minAge) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                    "申请 " + target + " 需年满 " + minAge + " 周岁，您目前 " + age + " 周岁");
        }

        // 3. 校验持有年限：用 license_obtained_date 自动计算
        if (!tracedBack && student.getLicenseObtainedDate() != null) {
            int yearsHeld = Period.between(student.getLicenseObtainedDate().toLocalDate(), LocalDate.now()).getYears();
            if (yearsHeld < matchedPath.years) {
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                        "增驾 " + target + " 需持有 " + original + " 满 " + matchedPath.years
                                + " 年，您于 " + student.getLicenseObtainedDate().toLocalDate()
                                + " 获得 " + original + "，目前仅持有 " + yearsHeld + " 年");
            }
        } else if (tracedBack) {
            // 追溯模式：通过已完成增驾记录的创建时间估算最低持有年限
            LicenseUpgrade prev = lambdaQuery()
                    .eq(LicenseUpgrade::getStudentId, student.getUserId())
                    .eq(LicenseUpgrade::getTargetLicense, student.getLicenseType())
                    .eq(LicenseUpgrade::getStatus, 1)
                    .eq(LicenseUpgrade::getExamStatus, 1)
                    .orderByDesc(LicenseUpgrade::getCreateTime)
                    .last("LIMIT 1")
                    .one();
            if (prev != null && prev.getCreateTime() != null) {
                int yearsHeld = Period.between(prev.getCreateTime().toLocalDate(), LocalDate.now()).getYears();
                if (yearsHeld < matchedPath.years) {
                    throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                            "追溯来源 " + original + " 持有年限不足，自 " + prev.getCreateTime().toLocalDate()
                                    + " 起持续持有，当前约 " + yearsHeld + " 年，增驾 " + target
                                    + " 需 " + matchedPath.years + " 年。"
                                    + "如实际持有年限已满足，请点击\"演示申请\"跳过校验");
                }
            }
        }
        // 注：tracedBack=true 但找不到参考记录时（极少发生），持有年限由管理员审核驾驶证材料人工判断
    }

    /**
     * 判断是否为同级车型
     */
    private boolean isSameLevel(String original, String target) {
        if (original.equals(target)) return true;
        if (GROUP_C.contains(original) && GROUP_C.contains(target)) return true;
        if (GROUP_B.contains(original) && GROUP_B.contains(target)) return true;
        if (GROUP_A_BUS.contains(original) && GROUP_A_BUS.contains(target)) return true;
        if (GROUP_MOTO.contains(original) && GROUP_MOTO.contains(target)) return true;
        if (GROUP_SPECIAL.contains(original) && GROUP_SPECIAL.contains(target)) return true;
        return false;
    }

    /**
     * 从身份证号计算年龄
     */
    private int calculateAgeFromIdCard(String idCard) {
        String birthStr = idCard.substring(6, 14);
        LocalDate birthDate = LocalDate.parse(birthStr, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
