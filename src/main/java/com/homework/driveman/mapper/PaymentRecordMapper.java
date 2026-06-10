package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.driveman.entity.PaymentRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 支付记录表 Mapper */
@Repository
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {

    /** 近12个月月度收入聚合（已支付，支持按年份筛选） */
    @Select("<script>" +
            "SELECT DATE_FORMAT(pay_time, '%Y-%m') AS month, SUM(amount) AS total " +
            "FROM payment_record " +
            "WHERE status = 1 AND is_deleted = 0 " +
            "  <if test='year != null'>AND YEAR(pay_time) = #{year}</if> " +
            "  <if test='year == null'>AND pay_time >= DATE_SUB(NOW(), INTERVAL 12 MONTH)</if> " +
            "GROUP BY DATE_FORMAT(pay_time, '%Y-%m') " +
            "ORDER BY month ASC" +
            "</script>")
    List<Map<String, Object>> selectMonthlyRevenue(@Param("year") Integer year);

    /** 收入按业务类型分布（支持按年份筛选；不传年份时为当月） */
    @Select("<script>" +
            "SELECT biz_type, SUM(amount) AS total, COUNT(*) AS count " +
            "FROM payment_record " +
            "WHERE status = 1 AND is_deleted = 0 " +
            "  <if test='year != null'>AND YEAR(pay_time) = #{year}</if> " +
            "  <if test='year == null'>AND YEAR(pay_time) = YEAR(NOW()) AND MONTH(pay_time) = MONTH(NOW())</if> " +
            "GROUP BY biz_type" +
            "</script>")
    List<Map<String, Object>> selectRevenueByBizType(@Param("year") Integer year);

    /** 总体收支汇总 */
    @Select("SELECT " +
            "  COALESCE(SUM(CASE WHEN status = 1 THEN amount ELSE 0 END), 0) AS total_paid, " +
            "  COUNT(CASE WHEN status = 1 THEN 1 END) AS paid_count, " +
            "  COALESCE(SUM(CASE WHEN status = 0 THEN amount ELSE 0 END), 0) AS total_pending, " +
            "  COUNT(CASE WHEN status = 0 THEN 1 END) AS pending_count, " +
            "  COALESCE(SUM(CASE WHEN status = 2 THEN amount ELSE 0 END), 0) AS total_refunded, " +
            "  COUNT(CASE WHEN status = 2 THEN 1 END) AS refunded_count " +
            "FROM payment_record WHERE is_deleted = 0")
    Map<String, Object> selectPaymentSummary();

    /** 欠费清单（待支付记录 + 学员姓名） */
    @Select("SELECT p.*, u.real_name AS student_name, u.phone AS student_phone, u.license_type " +
            "FROM payment_record p " +
            "JOIN user u ON p.student_id = u.user_id " +
            "WHERE p.status = 0 AND p.is_deleted = 0 AND u.is_deleted = 0 " +
            "ORDER BY p.create_time DESC")
    List<Map<String, Object>> selectOutstandingList();
}
