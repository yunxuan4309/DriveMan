package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.LicenseUpgrade;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.LicenseUpgradeMapper;
import com.homework.driveman.service.ILicenseUpgradeService;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.web.ServiceCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 增驾申请服务实现
 * 依据：《机动车驾驶证申领和使用规定》（公安部令第172号，2025年1月1日起施行）
 */
@Slf4j
@Service
public class LicenseUpgradeServiceImpl extends ServiceImpl<LicenseUpgradeMapper, LicenseUpgrade> implements ILicenseUpgradeService {

    @Autowired
    private IUserService userService;

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

    /** 升级路径内部类 */
    private static class UpgradePath {
        String source;
        int years;
        UpgradePath(String source, int years) { this.source = source; this.years = years; }
    }

    // ==================== 业务方法 ====================

    @Override
    public LicenseUpgrade apply(Integer studentId, String targetLicense, Integer upgradeType, Integer licenseFileId) {
        User student = userService.getById(studentId);
        if (student == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "学员不存在");
        }
        if (student.getLicenseType() == null) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "您尚未报名，无法申请增驾");
        }

        String originalLicense = student.getLicenseType();

        // 检查是否已有进行中的申请
        Long count = lambdaQuery()
                .eq(LicenseUpgrade::getStudentId, studentId)
                .in(LicenseUpgrade::getStatus, 0, 1)
                .count();
        if (count > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "您已有进行中的增驾申请");
        }

        // 校验目标车型合法性
        if (!MIN_AGE.containsKey(targetLicense)) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "无效的目标车型: " + targetLicense);
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
            validateUpgradeBasicConditions(student, originalLicense, targetLicense);
            if (licenseFileId == null) {
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
    public void audit(Integer id, Integer status, String remark) {
        LicenseUpgrade upgrade = getById(id);
        if (upgrade == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "增驾申请不存在");
        }
        if (upgrade.getStatus() != 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该申请已处理，无法重复审核");
        }

        upgrade.setStatus(status);
        upgrade.setRemark(remark);
        updateById(upgrade);

        // 审核通过后，状态变为"审核通过待考试"，不直接更新车型
        if (status == 1) {
            log.info("增驾申请审核通过，待考试: id={}, studentId={}", id, upgrade.getStudentId());
        }

        log.info("增驾申请审核完成: id={}, status={}", id, status);
    }

    @Override
    public void recordExamResult(Integer id, Integer examStatus, String examRemark) {
        LicenseUpgrade upgrade = getById(id);
        if (upgrade == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "增驾申请不存在");
        }
        if (upgrade.getStatus() != 1) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该申请未通过审核，无法录入考试成绩");
        }
        if (upgrade.getExamStatus() != null && upgrade.getExamStatus() == 1) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该申请已考试通过，无需重复录入");
        }

        upgrade.setExamStatus(examStatus);
        upgrade.setExamRemark(examRemark);
        updateById(upgrade);

        // 考试通过后才更新学员的准驾车型
        if (examStatus == 1) {
            User student = userService.getById(upgrade.getStudentId());
            if (student != null) {
                student.setLicenseType(upgrade.getTargetLicense());
                userService.updateById(student);
                log.info("学员车型已更新: userId={}, {} -> {}",
                        student.getUserId(), upgrade.getOriginalLicense(), upgrade.getTargetLicense());
            }
        }

        log.info("增驾考试成绩录入: id={}, examStatus={}", id, examStatus);
    }

    // ==================== 校验方法 ====================

    /**
     * 升级增驾基础校验（提交申请时执行）
     * 持有年限由管理员通过审核驾驶证材料来判断
     */
    private void validateUpgradeBasicConditions(User student, String original, String target) {
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

        // 3. 校验是否为同一车型（无需增驾）
        if (original.equals(target)) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                    "您已持有 " + target + " 车型，无需增驾");
        }

        // 注：持有年限由管理员审核学员上传的驾驶证材料来判断
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
