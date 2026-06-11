-- ============================================
-- upgrade_file_biztype.sql — 补充首批文件记录的 biz_type/file_size/mime_type
-- 说明：mass_test_data.sql 第一批 12 条 INSERT 缺少这些字段，
--       导致前端业务类型列显示为 "-"
-- 用法: mysql -u root -proot driveman < database/upgrade/upgrade_file_biztype.sql
-- ============================================

-- 赵强 (user_id=6)
UPDATE `file` SET biz_type='user_profile',     file_size=102400, mime_type='image/jpeg'      WHERE user_id=6 AND file_path='id_card_front/6_20260523001.jpg';
UPDATE `file` SET biz_type='user_profile',     file_size=98500,  mime_type='image/jpeg'      WHERE user_id=6 AND file_path='id_card_back/6_20260523002.jpg';
UPDATE `file` SET biz_type='physical_exam',    file_size=204800, mime_type='application/pdf', biz_id=1 WHERE user_id=6 AND file_path='physical_exam/6_20260523003.pdf';
UPDATE `file` SET biz_type='registration_form', file_size=152000, mime_type='application/pdf', biz_id=6 WHERE user_id=6 AND file_path='registration_pdf/6_registration.pdf';
UPDATE `file` SET biz_type='exam_ticket',       file_size=128000, mime_type='application/pdf', biz_id=6 WHERE user_id=6 AND file_path='admission_ticket/6_ticket.pdf';

-- 陈静 (user_id=7)
UPDATE `file` SET biz_type='user_profile',     file_size=102400, mime_type='image/jpeg'      WHERE user_id=7 AND file_path='id_card_front/7_20260524001.jpg';
UPDATE `file` SET biz_type='user_profile',     file_size=98500,  mime_type='image/jpeg'      WHERE user_id=7 AND file_path='id_card_back/7_20260524002.jpg';
UPDATE `file` SET biz_type='physical_exam',    file_size=204800, mime_type='application/pdf', biz_id=2 WHERE user_id=7 AND file_path='physical_exam/7_20260524003.pdf';

-- 刘洋 (user_id=8)
UPDATE `file` SET biz_type='user_profile',     file_size=102400, mime_type='image/jpeg'      WHERE user_id=8 AND file_path='id_card_front/8_20260525001.jpg';
UPDATE `file` SET biz_type='user_profile',     file_size=98500,  mime_type='image/jpeg'      WHERE user_id=8 AND file_path='id_card_back/8_20260525002.jpg';
UPDATE `file` SET biz_type='physical_exam',    file_size=210000, mime_type='image/png'       WHERE user_id=8 AND file_path='physical_exam/8_20260525003.png';

-- 周婷 (user_id=9)
UPDATE `file` SET biz_type='user_profile',     file_size=102400, mime_type='image/jpeg'      WHERE user_id=9 AND file_path='id_card_front/9_20260526001.jpg';

-- 校验
SELECT id, user_id, file_name, file_type, biz_type, file_size, mime_type FROM `file` ORDER BY id;
