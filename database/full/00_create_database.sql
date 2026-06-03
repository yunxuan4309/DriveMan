-- ============================================
-- 00 — 创建数据库
-- 说明: 删除旧库并重新创建 driveman 数据库
-- 执行: mysql -u root -proot < 00_create_database.sql
-- ============================================

DROP DATABASE IF EXISTS `driveman`;
CREATE DATABASE IF NOT EXISTS `driveman`
CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;

USE `driveman`;
