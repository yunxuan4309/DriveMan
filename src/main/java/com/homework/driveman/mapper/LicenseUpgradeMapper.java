package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.entity.LicenseUpgrade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 增驾申请表 Mapper
 */
@Mapper
public interface LicenseUpgradeMapper extends BaseMapper<LicenseUpgrade> {

    /**
     * 分页查询增驾申请（含学员姓名），支持多条件搜索
     */
    @Select("<script>" +
            "SELECT lu.id, lu.student_id AS studentId, lu.original_license AS originalLicense, " +
            "       lu.target_license AS targetLicense, lu.upgrade_type AS upgradeType, " +
            "       lu.status, lu.remark, lu.exam_status AS examStatus, lu.exam_remark AS examRemark, " +
            "       lu.skip_subjects AS skipSubjects, " +
            "       lu.license_file_id AS licenseFileId, lu.create_time AS createTime, " +
            "       u.real_name AS studentName " +
            "FROM license_upgrade lu " +
            "LEFT JOIN user u ON lu.student_id = u.user_id " +
            "WHERE lu.is_deleted = 0 " +
            "  <if test='keyword != null and keyword != \"\"'>AND u.real_name LIKE CONCAT('%', #{keyword}, '%')</if> " +
            "  <if test='originalLicense != null and originalLicense != \"\"'>AND lu.original_license = #{originalLicense}</if> " +
            "  <if test='targetLicense != null and targetLicense != \"\"'>AND lu.target_license = #{targetLicense}</if> " +
            "  <if test='status != null'>AND lu.status = #{status}</if> " +
            "  <if test='examStatus != null'>AND lu.exam_status = #{examStatus}</if> " +
            "  <if test='createTimeStart != null'>AND lu.create_time &gt;= #{createTimeStart}</if> " +
            "  <if test='createTimeEnd != null'>AND lu.create_time &lt;= #{createTimeEnd}</if> " +
            "ORDER BY lu.create_time DESC" +
            "</script>")
    Page<Map<String, Object>> selectPageWithDetails(Page<?> page,
                                                     @Param("keyword") String keyword,
                                                     @Param("originalLicense") String originalLicense,
                                                     @Param("targetLicense") String targetLicense,
                                                     @Param("status") Integer status,
                                                     @Param("examStatus") Integer examStatus,
                                                     @Param("createTimeStart") LocalDateTime createTimeStart,
                                                     @Param("createTimeEnd") LocalDateTime createTimeEnd);

    /**
     * 查询学员针对某目标车型已通过的考试科目
     */
    @Select("SELECT DISTINCT er.subject FROM exam_registration er " +
            "JOIN exam_session es ON er.session_id = es.id " +
            "WHERE er.student_id = #{studentId} AND es.license_type = #{licenseType} " +
            "AND er.status = 3 AND er.pass_status = 1 AND er.is_deleted = 0")
    List<Integer> selectPassedSubjects(@Param("studentId") Integer studentId,
                                       @Param("licenseType") String licenseType);

    /**
     * 查询学员针对某目标车型某科目是否有待审核/已通过的考试报名
     */
    @Select("SELECT COUNT(*) FROM exam_registration er " +
            "JOIN exam_session es ON er.session_id = es.id " +
            "WHERE er.student_id = #{studentId} AND es.license_type = #{licenseType} " +
            "AND er.subject = #{subject} AND er.status IN (0, 1) AND er.is_deleted = 0")
    int countPendingRegistration(@Param("studentId") Integer studentId,
                                 @Param("licenseType") String licenseType,
                                 @Param("subject") Integer subject);
}
