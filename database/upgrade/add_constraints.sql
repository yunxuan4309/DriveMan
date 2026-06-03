-- ============================================
-- 添加外键约束（建议在开发后期执行）
-- ============================================

USE driveman;

ALTER TABLE `coach`
    ADD CONSTRAINT `fk_coach_user`
        FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
            ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `student_coach`
    ADD CONSTRAINT `fk_sc_student`
        FOREIGN KEY (`student_id`) REFERENCES `user`(`user_id`)
            ON DELETE RESTRICT ON UPDATE CASCADE,
ADD CONSTRAINT `fk_sc_coach`
FOREIGN KEY (`coach_id`) REFERENCES `coach`(`coach_id`)
ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `appointment`
    ADD CONSTRAINT `fk_app_student`
        FOREIGN KEY (`student_id`) REFERENCES `user`(`user_id`)
            ON DELETE RESTRICT ON UPDATE CASCADE,
ADD CONSTRAINT `fk_app_coach`
FOREIGN KEY (`coach_id`) REFERENCES `coach`(`coach_id`)
ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `training_record`
    ADD CONSTRAINT `fk_tr_student`
        FOREIGN KEY (`student_id`) REFERENCES `user`(`user_id`)
            ON DELETE RESTRICT ON UPDATE CASCADE,
ADD CONSTRAINT `fk_tr_coach`
FOREIGN KEY (`coach_id`) REFERENCES `coach`(`coach_id`)
ON DELETE RESTRICT ON UPDATE CASCADE,
ADD CONSTRAINT `fk_tr_appointment`
FOREIGN KEY (`appointment_id`) REFERENCES `appointment`(`id`)
ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `exam_registration`
    ADD CONSTRAINT `fk_er_student`
        FOREIGN KEY (`student_id`) REFERENCES `user`(`user_id`)
            ON DELETE RESTRICT ON UPDATE CASCADE,
ADD CONSTRAINT `fk_er_session`
FOREIGN KEY (`session_id`) REFERENCES `exam_session`(`id`)
ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `file`
    ADD CONSTRAINT `fk_file_user`
        FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
            ON DELETE CASCADE ON UPDATE CASCADE;