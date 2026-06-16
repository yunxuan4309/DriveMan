package com.homework.driveman.vo;

import com.homework.driveman.entity.User;
import lombok.Data;

/**
 * 学员列表视图对象 — 继承 User 所有字段，额外携带结业状态 + 教练绑定信息
 */
@Data
public class StudentListVO extends User {
    /** 是否全部科目已通过（结业） */
    private Boolean allPassed;

    /** 已通过科目数 */
    private Integer passedCount;

    /** 总科目数 */
    private Integer totalSubjects;

    /** 当前绑定的教练ID（未分配时为 null） */
    private Integer coachId;

    /** 当前绑定的教练姓名 */
    private String coachName;

    /** student_coach 绑定记录ID（用于执行解绑操作） */
    private Integer bindId;
}
