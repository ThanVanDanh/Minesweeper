-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: minesweeper_db
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `audit_log`
--

DROP TABLE IF EXISTS `audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `admin_id` bigint unsigned DEFAULT NULL,
  `action` varchar(50) NOT NULL,
  `target` varchar(255) NOT NULL,
  `details` text,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_audit_log_admin` (`admin_id`),
  KEY `idx_audit_created` (`created_at` DESC),
  KEY `idx_audit_action` (`action`),
  CONSTRAINT `fk_audit_log_admin` FOREIGN KEY (`admin_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_log`
--

LOCK TABLES `audit_log` WRITE;
/*!40000 ALTER TABLE `audit_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `email_verification_tokens`
--

DROP TABLE IF EXISTS `email_verification_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `email_verification_tokens` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `email` varchar(255) NOT NULL,
  `otp_hash` varchar(255) NOT NULL,
  `expires_at` timestamp NOT NULL,
  `is_used` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_evt_user` (`user_id`),
  CONSTRAINT `fk_evt_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `email_verification_tokens`
--

LOCK TABLES `email_verification_tokens` WRITE;
/*!40000 ALTER TABLE `email_verification_tokens` DISABLE KEYS */;
/*!40000 ALTER TABLE `email_verification_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `game_levels`
--

DROP TABLE IF EXISTS `game_levels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `game_levels` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `level_name` varchar(50) NOT NULL,
  `rows_count` int unsigned NOT NULL,
  `cols_count` int unsigned NOT NULL,
  `mines_count` int unsigned NOT NULL,
  `sort_order` tinyint unsigned NOT NULL DEFAULT '0',
  `level_type` enum('PRESET','CUSTOM') NOT NULL DEFAULT 'PRESET',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `level_name` (`level_name`),
  KEY `idx_level_name` (`level_name`),
  CONSTRAINT `chk_game_levels_board_size` CHECK (((`rows_count` > 0) and (`cols_count` > 0))),
  CONSTRAINT `chk_game_levels_mines_count` CHECK (((`mines_count` > 0) and (`mines_count` < (`rows_count` * `cols_count`))))
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `game_levels`
--

LOCK TABLES `game_levels` WRITE;
/*!40000 ALTER TABLE `game_levels` DISABLE KEYS */;
INSERT INTO `game_levels` VALUES (1,'EASY',9,9,10,1,'PRESET','2026-05-09 07:04:50'),(2,'MEDIUM',16,16,40,2,'PRESET','2026-05-09 07:04:50'),(3,'HARD',16,30,99,3,'PRESET','2026-05-09 07:04:50'),(4,'EXPERT',20,30,145,4,'PRESET','2026-05-09 07:04:50'),(5,'CUSTOM',10,10,10,5,'CUSTOM','2026-05-09 07:04:50');
/*!40000 ALTER TABLE `game_levels` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `game_sessions`
--

DROP TABLE IF EXISTS `game_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `game_sessions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `level_id` int unsigned NOT NULL,
  `result` enum('WIN','LOSE') NOT NULL,
  `completion_time` bigint unsigned NOT NULL DEFAULT '0',
  `score` int NOT NULL DEFAULT '0',
  `opened_cells` int unsigned NOT NULL DEFAULT '0',
  `flagged_cells` int unsigned NOT NULL DEFAULT '0',
  `started_at` timestamp NULL DEFAULT NULL,
  `first_click_at` timestamp NULL DEFAULT NULL,
  `ended_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_result` (`user_id`,`result`),
  KEY `idx_user_level_created` (`user_id`,`level_id`,`created_at` DESC),
  KEY `idx_level_result_time` (`level_id`,`result`,`completion_time`),
  KEY `idx_level_score` (`level_id`,`score` DESC),
  KEY `idx_created` (`created_at` DESC),
  CONSTRAINT `fk_game_sessions_level` FOREIGN KEY (`level_id`) REFERENCES `game_levels` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_game_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_game_sessions_cells` CHECK (((`opened_cells` >= 0) and (`flagged_cells` >= 0))),
  CONSTRAINT `chk_game_sessions_datetime` CHECK (((`first_click_at` is null) or (`ended_at` is null) or (`ended_at` >= `first_click_at`))),
  CONSTRAINT `chk_game_sessions_time` CHECK ((`completion_time` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `game_sessions`
--

LOCK TABLES `game_sessions` WRITE;
/*!40000 ALTER TABLE `game_sessions` DISABLE KEYS */;
INSERT INTO `game_sessions` VALUES (1,1,1,'LOSE',36000,0,1,0,'2026-05-09 00:35:42','2026-05-09 00:36:17','2026-05-09 00:36:19','2026-05-09 07:36:18','2026-05-09 07:36:18'),(2,1,1,'WIN',55000,1843,71,0,'2026-05-10 02:46:32','2026-05-10 02:46:33','2026-05-10 02:47:26','2026-05-10 09:47:26','2026-05-10 09:47:26'),(3,1,1,'LOSE',0,0,1,0,'2026-05-10 04:04:25','2026-05-10 04:04:26','2026-05-10 04:04:28','2026-05-10 11:04:28','2026-05-10 11:04:28'),(27,21,4,'WIN',32000,98420,450,145,'2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14'),(28,21,4,'WIN',35000,97100,448,145,'2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14'),(29,21,4,'LOSE',45000,95000,400,140,'2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14'),(30,22,4,'WIN',38000,87310,440,145,'2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14'),(31,22,4,'WIN',40000,86500,438,145,'2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14'),(32,23,4,'WIN',41000,76100,430,144,'2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14'),(33,23,4,'LOSE',55000,70000,350,130,'2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14'),(34,24,4,'WIN',45000,65200,420,143,'2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14'),(35,24,4,'LOSE',60000,60000,340,125,'2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14'),(36,25,4,'WIN',48000,54800,410,142,'2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14'),(37,25,4,'LOSE',65000,50000,330,120,'2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14','2026-06-03 09:58:14');
/*!40000 ALTER TABLE `game_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `player_best_scores`
--

DROP TABLE IF EXISTS `player_best_scores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `player_best_scores` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `level_id` int unsigned NOT NULL,
  `best_time` bigint unsigned NOT NULL,
  `best_score` int NOT NULL DEFAULT '0',
  `game_session_id` bigint unsigned NOT NULL,
  `achieved_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_player_best_scores_user_level` (`user_id`,`level_id`),
  KEY `fk_player_best_scores_session` (`game_session_id`),
  KEY `idx_level_rank` (`level_id`,`best_time`,`best_score` DESC,`achieved_at`),
  CONSTRAINT `fk_player_best_scores_level` FOREIGN KEY (`level_id`) REFERENCES `game_levels` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_player_best_scores_session` FOREIGN KEY (`game_session_id`) REFERENCES `game_sessions` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_player_best_scores_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_player_best_scores_time` CHECK ((`best_time` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `player_best_scores`
--

LOCK TABLES `player_best_scores` WRITE;
/*!40000 ALTER TABLE `player_best_scores` DISABLE KEYS */;
INSERT INTO `player_best_scores` VALUES (1,1,1,55000,1843,2,'2026-05-10 09:47:26');
/*!40000 ALTER TABLE `player_best_scores` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ranking_snapshots`
--

DROP TABLE IF EXISTS `ranking_snapshots`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ranking_snapshots` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `level_id` int unsigned DEFAULT NULL,
  `rank_position` int unsigned NOT NULL,
  `best_score` int NOT NULL DEFAULT '0',
  `best_time_ms` bigint unsigned NOT NULL DEFAULT '0',
  `snapshot_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_snapshot_user_level` (`user_id`,`level_id`,`snapshot_at` DESC),
  KEY `idx_snapshot_level_at` (`level_id`,`snapshot_at` DESC),
  CONSTRAINT `fk_ranking_snapshots_level` FOREIGN KEY (`level_id`) REFERENCES `game_levels` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_ranking_snapshots_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ranking_snapshots`
--

LOCK TABLES `ranking_snapshots` WRITE;
/*!40000 ALTER TABLE `ranking_snapshots` DISABLE KEYS */;
/*!40000 ALTER TABLE `ranking_snapshots` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(100) NOT NULL,
  `display_name` varchar(100) DEFAULT NULL,
  `password_hash` varchar(255) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `role` enum('PLAYER','ADMIN') NOT NULL DEFAULT 'PLAYER',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_login_at` timestamp NULL DEFAULT NULL,
  `remember_token` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'Player','Player',NULL,NULL,'PLAYER',1,'2026-05-09 07:36:18','2026-05-09 07:36:18',NULL,NULL),(2,'thvandanh','thvandanh','c2a042bdf745f89e0db523b2c8e44fac',NULL,'PLAYER',1,'2026-05-09 08:10:59','2026-05-09 08:11:08','2026-05-09 08:11:08',NULL),(3,'hihihi','Văn Danh','101a6ec9f938885df0a44f20458d2eb4',NULL,'ADMIN',1,'2026-05-10 00:39:01','2026-06-03 10:05:39','2026-06-03 10:05:39',NULL),(4,'danhhh','Van Danh','101a6ec9f938885df0a44f20458d2eb4',NULL,'PLAYER',1,'2026-05-10 00:59:35','2026-05-10 01:00:29',NULL,NULL),(5,'baobao','Bảo Bảo','7337e2f117b38edd90ef8ddd50c31406',NULL,'PLAYER',1,'2026-05-10 09:08:35','2026-05-10 09:09:25','2026-05-10 09:09:25',NULL),(6,'sad','adas@@@','3fcc37c6c47a33d46fdb447590b73ba2',NULL,'PLAYER',1,'2026-05-10 15:20:46','2026-05-10 15:22:56','2026-05-10 15:22:50',NULL),(7,'vdanh','Văn Danh','b3c283f1dbd83b4e303624631d7b8de9',NULL,'PLAYER',1,'2026-05-10 15:23:54','2026-05-10 15:25:17','2026-05-10 15:24:30',NULL),(8,'vdanh22','Văn Danh','127aca9ad0a6f7d5f4da3da7f889857e',NULL,'PLAYER',1,'2026-05-10 16:20:51','2026-05-10 16:21:50','2026-05-10 16:21:02',NULL),(9,'vandanh2207','Thân Văn Danh','1f4c8b313e1ba55eddcd59edc081c5ba',NULL,'PLAYER',1,'2026-05-10 16:29:48','2026-05-10 16:30:53','2026-05-10 16:30:14',NULL),(10,'vandanh','Danh','d20c1243a9ca28d96fbac8be8e639d3a',NULL,'PLAYER',1,'2026-05-17 15:29:58','2026-05-17 15:30:38','2026-05-17 15:30:14',NULL),(21,'user1','NightSapper','hash1','user1@test.com','PLAYER',1,'2026-06-03 09:58:12','2026-06-03 09:58:12',NULL,NULL),(22,'user2','BombDisposer','hash2','user2@test.com','PLAYER',1,'2026-06-03 09:58:12','2026-06-03 09:58:12',NULL,NULL),(23,'user3','GridMaster_VN','hash3','user3@test.com','PLAYER',1,'2026-06-03 09:58:12','2026-06-03 09:58:12',NULL,NULL),(24,'user4','QuantumSweep','hash4','user4@test.com','PLAYER',1,'2026-06-03 09:58:12','2026-06-03 09:58:12',NULL,NULL),(25,'user5','ShadowMiner','hash5','user5@test.com','PLAYER',1,'2026-06-03 09:58:12','2026-06-03 09:58:12',NULL,NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-03 21:30:21
