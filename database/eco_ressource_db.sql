-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : mer. 29 avr. 2026 à 23:32
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `eco_ressource_dbb.sql`
--

-- --------------------------------------------------------

--
-- Structure de la table `comments`
--

CREATE TABLE `comments` (
  `id` bigint(20) NOT NULL,
  `content` varchar(2000) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `listing_id` bigint(20) NOT NULL,
  `parent_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) NOT NULL,
  `moderation_reason` varchar(500) DEFAULT NULL,
  `moderation_status` enum('VISIBLE','MASKED','BLOCKED') DEFAULT NULL,
  `toxicity_score` double DEFAULT NULL,
  `original_content` varchar(2000) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `comments`
--

INSERT INTO `comments` (`id`, `content`, `created_at`, `listing_id`, `parent_id`, `user_id`, `moderation_reason`, `moderation_status`, `toxicity_score`, `original_content`) VALUES
(10, 'Is this still available?', '2026-04-09 16:42:23.000000', 10, NULL, 2, NULL, NULL, NULL, NULL),
(11, 'Yes available', '2026-04-09 16:42:23.000000', 10, 10, 2, NULL, NULL, NULL, NULL),
(12, 'Can you ship to Sfax?', '2026-04-09 16:42:23.000000', 10, NULL, 3, NULL, NULL, NULL, NULL),
(14, 'What is purity level?', '2026-04-09 16:42:23.000000', 11, NULL, 2, NULL, NULL, NULL, NULL),
(15, '95% recycled plastic', '2026-04-09 16:42:23.000000', 11, 14, 2, NULL, NULL, NULL, NULL),
(16, 'We can supply wood pallets', '2026-04-09 16:42:23.000000', 13, NULL, 3, NULL, NULL, NULL, NULL),
(17, 'Interested in joining', '2026-04-09 16:42:23.000000', 15, NULL, 2, NULL, NULL, NULL, NULL),
(18, 'Me too', '2026-04-09 16:42:23.000000', 15, NULL, 3, NULL, NULL, NULL, NULL),
(61, 'SALUT', '2026-04-22 20:17:25.000000', 26, NULL, 5, NULL, NULL, NULL, NULL),
(62, 'SALUT', '2026-04-22 20:17:33.000000', 26, NULL, 2, NULL, NULL, NULL, NULL),
(76, 'hello', '2026-04-26 16:48:17.000000', 12, NULL, 2, 'Score de toxicite Perspective: 0.019728716', 'VISIBLE', 0.019728716, NULL),
(77, 'hello', '2026-04-26 16:48:25.000000', 12, NULL, 2, 'Score de toxicite Perspective: 0.019728716', 'VISIBLE', 0.019728716, NULL),
(78, 'hello', '2026-04-26 16:48:31.000000', 12, NULL, 2, 'Score de toxicite Perspective: 0.019728716', 'VISIBLE', 0.019728716, NULL),
(79, 'Bonjour, le lot aluminium est-il disponible pour une livraison cette semaine ?', '2026-04-23 19:50:26.000000', 32, NULL, 9, NULL, 'VISIBLE', 0.05, NULL),
(80, 'Oui, le lot est disponible. Livraison possible apres validation du transport.', '2026-04-23 19:50:26.000000', 32, NULL, 2, NULL, 'VISIBLE', 0.03, NULL),
(81, 'Ce commentaire a ete masque en raison de toxicite moyenne.', '2026-04-24 19:50:26.000000', 32, NULL, 7, 'Toxicite moyenne detectee par la moderation automatique.', 'MASKED', 0.56, NULL),
(82, 'Ce commentaire a ete supprime en raison de toxicite elevee.', '2026-04-25 19:50:26.000000', 32, NULL, 9, 'Toxicite elevee detectee par la moderation automatique.', 'BLOCKED', 0.91, NULL),
(83, 'Nous sommes interesses par 900 kg de PET si la livraison est mutualisee.', '2026-04-24 19:50:26.000000', 34, NULL, 7, NULL, 'VISIBLE', 0.02, NULL),
(84, 'Le prix peut-il rester a 1.30 TND/kg si le groupe atteint 5 tonnes ?', '2026-04-25 19:50:26.000000', 34, NULL, 2, NULL, 'VISIBLE', 0.04, NULL),
(85, 'Lot textile interessant, est-ce que les couleurs sont deja separees ?', '2026-04-26 11:50:26.000000', 35, NULL, 9, NULL, 'VISIBLE', 0.03, NULL),
(86, 'Merci, pouvez-vous envoyer une estimation du transport vers Sousse ?', '2026-04-24 19:50:26.000000', 32, 79, 9, NULL, 'VISIBLE', 0.01, NULL),
(87, 'Oui, si le seuil est atteint le prix reste bloque pour les participants.', '2026-04-25 23:50:26.000000', 34, 83, 9, NULL, 'VISIBLE', 0.02, NULL),
(88, 'Commentaire test 195', '2026-04-24 19:52:13.000000', 32, NULL, 9, NULL, 'VISIBLE', 0.23427453565809367, NULL),
(89, 'Commentaire test 227', '2026-04-25 19:52:13.000000', 32, NULL, 9, NULL, 'VISIBLE', 0.379582841302811, NULL),
(90, 'Commentaire test 937', '2026-04-25 19:52:13.000000', 32, NULL, 9, NULL, 'VISIBLE', 0.041216439605892115, NULL),
(103, 'hi', '2026-04-26 18:54:17.000000', 52, NULL, 2, 'Score de toxicite Perspective: 0.017341165', 'VISIBLE', 0.017341165, NULL),
(104, 'jjjjjjjjjjj', '2026-04-26 19:13:14.000000', 52, NULL, 2, 'Score de toxicite Perspective: 0.018723432', 'VISIBLE', 0.018723432, NULL),
(108, 'Ce commentaire a ete masque en raison de toxicite moyenne.', '2026-04-27 21:45:30.000000', 89, NULL, 2, 'Score de toxicite Perspective: 0.50789946', 'MASKED', 0.50789946, NULL),
(109, 'livraison possible', '2026-04-28 16:09:36.000000', 10, 12, 2, 'Score de toxicite Perspective: 0.0103670005', 'VISIBLE', 0.0103670005, NULL),
(112, 'Ce commentaire a ete masque en raison de toxicite moyenne.', '2026-04-28 17:33:40.000000', 92, NULL, 2, 'Score de toxicite Perspective: 0.5885171', 'MASKED', 0.5885171, NULL),
(114, 'hello', '2026-04-28 17:50:28.000000', 34, NULL, 5, 'Score de toxicite Perspective: 0.019728716', 'VISIBLE', 0.019728716, NULL),
(116, 'disponible', '2026-04-29 13:00:24.000000', 92, NULL, 2, 'Score de toxicite Perspective: 0.015204934', 'VISIBLE', 0.015204934, NULL),
(117, 'oui', '2026-04-29 13:00:34.000000', 92, 116, 5, 'Score de toxicite Perspective: 0.005529067', 'VISIBLE', 0.005529067, NULL),
(129, 'Ce commentaire a ete masque en raison de toxicite moyenne.', '2026-04-29 20:04:52.000000', 91, NULL, 2, 'Score de toxicite Perspective: 0.5885171', 'MASKED', 0.5885171, 'putain putain putain'),
(131, 'Ce commentaire a ete masque en raison de toxicite moyenne.', '2026-04-29 20:22:16.000000', 50, NULL, 2, 'Score de toxicite Perspective: 0.73663366', 'MASKED', 0.73663366, 'mort putain putain');

-- --------------------------------------------------------

--
-- Structure de la table `deliveries`
--

CREATE TABLE `deliveries` (
  `id` bigint(20) NOT NULL,
  `amount` decimal(14,2) DEFAULT NULL,
  `client_name` varchar(255) DEFAULT NULL,
  `co2label` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `date_label` varchar(255) DEFAULT NULL,
  `delivery_label` varchar(255) DEFAULT NULL,
  `earn_amount` decimal(14,2) DEFAULT NULL,
  `from_location` varchar(255) NOT NULL,
  `pickup_label` varchar(255) DEFAULT NULL,
  `product_label` varchar(255) NOT NULL,
  `status` enum('delivered','in_transit','pickup','pending','scheduled','transit') NOT NULL,
  `to_location` varchar(255) NOT NULL,
  `enterprise_id` bigint(20) NOT NULL,
  `transporter_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `deliveries`
--

INSERT INTO `deliveries` (`id`, `amount`, `client_name`, `co2label`, `created_at`, `date_label`, `delivery_label`, `earn_amount`, `from_location`, `pickup_label`, `product_label`, `status`, `to_location`, `enterprise_id`, `transporter_id`) VALUES
(1, 1200.00, 'Industrie Slim', '12kg', '2026-04-09 15:23:41.000000', 'Mar 10', NULL, 190.00, 'Tunis', NULL, 'Aluminum Scrap 2T', 'delivered', 'Sfax', 1, 1);

-- --------------------------------------------------------

--
-- Structure de la table `delivery_orders`
--

CREATE TABLE `delivery_orders` (
  `id_delivery` bigint(20) NOT NULL,
  `adresse_livraison` varchar(255) NOT NULL,
  `date_prevue` datetime(6) NOT NULL,
  `nom_client` varchar(100) NOT NULL,
  `statut` enum('EN_ATTENTE','EN_COURS','LIVREE') NOT NULL,
  `telephone_client` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `delivery_orders`
--

INSERT INTO `delivery_orders` (`id_delivery`, `adresse_livraison`, `date_prevue`, `nom_client`, `statut`, `telephone_client`) VALUES
(11, 'sfax', '2026-05-01 11:34:02.000000', 'slim ben ali', 'LIVREE', '55896478'),
(12, 'sfax ', '2026-04-30 11:47:16.000000', 'slim ben ali', 'LIVREE', '95275001'),
(13, 'le kef ', '2026-04-30 11:52:18.000000', 'slim ben ali', 'LIVREE', '54789632');

-- --------------------------------------------------------

--
-- Structure de la table `enterprises`
--

CREATE TABLE `enterprises` (
  `id` bigint(20) NOT NULL,
  `company_name` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `listings_count` int(11) NOT NULL,
  `orders_count` int(11) NOT NULL,
  `revenue` varchar(255) DEFAULT NULL,
  `sector` varchar(255) DEFAULT NULL,
  `tax_id` varchar(255) DEFAULT NULL,
  `user_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `enterprises`
--

INSERT INTO `enterprises` (`id`, `company_name`, `created_at`, `listings_count`, `orders_count`, `revenue`, `sector`, `tax_id`, `user_id`) VALUES
(1, 'Industrie Slim SARL', '2026-04-09 15:20:15.000000', 7, 12, '14200', 'Metallurgy', 'TN123', 2),
(2, 'Ma société', '2026-04-22 10:39:58.000000', 0, 0, '0', 'Recyclage', 'TN12345678', 4),
(3, 'ENTREPRISE1', '2026-04-22 20:02:53.000000', 0, 0, '0', 'Textile', '123456/A', 5),
(4, 'Mona Textile SA', '2026-04-26 19:50:25.000000', 2, 3, '3200', 'Textile & recyclage', 'TN-TEST-TEXT', 6),
(5, 'Nader Plastique SARL', '2026-04-26 19:50:25.000000', 2, 8, '7600', 'Plastique recycle', 'TN-TEST-PLAST', 7),
(6, 'Amira Recyclage', '2026-04-26 19:50:25.000000', 1, 5, '5400', 'Verre et emballage', 'TN-TEST-VERRE', 8),
(7, 'Industrie Slim SARL', '2026-04-27 19:58:19.000000', 1, 12, '14200', 'Metallurgy', 'TN123', 9);

-- --------------------------------------------------------

--
-- Structure de la table `escrow`
--

CREATE TABLE `escrow` (
  `idescrow` bigint(20) NOT NULL,
  `amount` double DEFAULT NULL,
  `created_at` varchar(255) DEFAULT NULL,
  `delivery_order_id` bigint(20) DEFAULT NULL,
  `enterprise_id` bigint(20) DEFAULT NULL,
  `id_stock` bigint(20) DEFAULT NULL,
  `linked_invoice_id` bigint(20) DEFAULT NULL,
  `project` varchar(255) DEFAULT NULL,
  `release_date` varchar(255) DEFAULT NULL,
  `status` enum('LOCKED','RELEASED','DISPUTED') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `escrow`
--

INSERT INTO `escrow` (`idescrow`, `amount`, `created_at`, `delivery_order_id`, `enterprise_id`, `id_stock`, `linked_invoice_id`, `project`, `release_date`, `status`) VALUES
(11, 238, '2026-04-23', NULL, NULL, NULL, 10, 'sqqsssd', '2026-04-23', 'RELEASED'),
(12, 119, '2026-04-23', NULL, NULL, NULL, 11, 'skmkm', '2026-04-23', 'RELEASED'),
(13, 238, '2026-04-23', NULL, NULL, NULL, 12, 'csfyy', '2026-04-23', 'RELEASED'),
(14, 1000, '2026-02-10', NULL, NULL, NULL, NULL, 'dcdf', NULL, 'DISPUTED'),
(15, 476, '2026-04-23', NULL, NULL, NULL, NULL, 'qqq', '2026-04-23', 'LOCKED'),
(16, 142.8, '2026-04-23', NULL, NULL, NULL, 14, 'sirine', NULL, 'LOCKED'),
(17, 476, '2026-04-25', NULL, NULL, NULL, 15, 'linda ', NULL, 'LOCKED'),
(18, 476, '2026-04-25', NULL, NULL, NULL, 16, 'hjde', '2026-04-25', 'RELEASED'),
(19, 502.18, '2026-04-25', NULL, NULL, NULL, 17, 'qqq', NULL, 'LOCKED'),
(20, 833, '2026-04-25', NULL, NULL, NULL, 18, 'dsss', NULL, 'LOCKED'),
(21, 476, '2026-04-25', NULL, NULL, NULL, 19, 'ddh', NULL, 'LOCKED'),
(22, 476, '2026-04-25', NULL, 4, NULL, NULL, 'ddh', NULL, 'LOCKED'),
(23, 476, '2026-04-25', NULL, NULL, NULL, 20, 'qqqq', NULL, 'LOCKED'),
(24, 476, '2026-04-25', NULL, 4, NULL, NULL, 'qqqq', '2026-04-25', 'RELEASED'),
(25, 119, '2026-04-25', NULL, NULL, NULL, 21, 'hggiu', '2026-04-25', 'RELEASED'),
(26, 833, '2026-04-26', NULL, NULL, NULL, 22, 'abc', '2026-04-26', 'RELEASED'),
(27, 833, '2026-04-25', NULL, 4, NULL, NULL, 'abc', '2026-04-26', 'RELEASED'),
(28, 119, '2026-04-26', NULL, NULL, NULL, 23, 'qdqd', '2026-04-27', 'RELEASED'),
(29, 119, '2026-04-26', NULL, 4, NULL, NULL, 'qdqd', '2026-04-27', 'RELEASED'),
(30, 109, '2026-04-28', NULL, NULL, NULL, 24, 'sisi', '2026-04-28', 'RELEASED'),
(31, 1071, '2026-04-28', NULL, NULL, NULL, 25, 'izzz', '2026-04-28', 'RELEASED'),
(32, 1071, '2026-04-28', NULL, 4, NULL, NULL, 'izzz', '2026-04-28', 'RELEASED');

-- --------------------------------------------------------

--
-- Structure de la table `exchange_requests`
--

CREATE TABLE `exchange_requests` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `duration_label` varchar(255) NOT NULL,
  `from_avatar` varchar(255) NOT NULL,
  `from_company_name` varchar(255) NOT NULL,
  `from_date` date NOT NULL,
  `item` varchar(255) NOT NULL,
  `message` varchar(2000) NOT NULL,
  `price` decimal(14,2) NOT NULL,
  `received_label` varchar(255) NOT NULL,
  `status` enum('pending','accepted','declined') NOT NULL,
  `to_date` date NOT NULL,
  `type_label` varchar(255) NOT NULL,
  `urgent` bit(1) NOT NULL,
  `recipient_enterprise_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `exchange_requests`
--

INSERT INTO `exchange_requests` (`id`, `created_at`, `duration_label`, `from_avatar`, `from_company_name`, `from_date`, `item`, `message`, `price`, `received_label`, `status`, `to_date`, `type_label`, `urgent`, `recipient_enterprise_id`) VALUES
(1, '2026-04-09 15:23:41.000000', '2 days', 'TM', 'Textile Mona SA', '2025-03-25', 'CNC Milling Machine', 'We need your CNC machine for a short production run.', 400.00, '10 min ago', 'pending', '2025-03-27', 'Machine Rental', b'1', 1);

-- --------------------------------------------------------

--
-- Structure de la table `favorites`
--

CREATE TABLE `favorites` (
  `id` bigint(20) NOT NULL,
  `listing_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `favorites`
--

INSERT INTO `favorites` (`id`, `listing_id`, `user_id`) VALUES
(116, 10, 2),
(11, 11, 2),
(59, 12, 2),
(20, 13, 2),
(12, 15, 2),
(15, 16, 2),
(14, 17, 2),
(24, 18, 2),
(17, 19, 2),
(19, 20, 2),
(25, 21, 2),
(40, 22, 2),
(43, 23, 2),
(49, 26, 2),
(63, 34, 2),
(66, 36, 2),
(132, 42, 2),
(114, 52, 2),
(117, 89, 2),
(135, 92, 2),
(13, 16, 3),
(44, 25, 5),
(51, 26, 5),
(140, 50, 5),
(138, 91, 5),
(136, 92, 5),
(61, 32, 7),
(64, 34, 7),
(98, 39, 7),
(99, 40, 7),
(101, 42, 7),
(102, 43, 7),
(103, 44, 7),
(104, 45, 7),
(105, 46, 7),
(106, 47, 7),
(108, 49, 7),
(109, 50, 7),
(111, 52, 7),
(112, 53, 7),
(62, 32, 8),
(65, 35, 8),
(115, 89, 9);

-- --------------------------------------------------------

--
-- Structure de la table `financing_request`
--

CREATE TABLE `financing_request` (
  `id` bigint(20) NOT NULL,
  `amount_approved` double DEFAULT NULL,
  `amount_requested` double DEFAULT NULL,
  `duration_months` int(11) NOT NULL,
  `interest_rate` double DEFAULT NULL,
  `project_name` varchar(255) DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `group_participants`
--

CREATE TABLE `group_participants` (
  `id` bigint(20) NOT NULL,
  `company_id` bigint(20) NOT NULL,
  `quantity` int(11) NOT NULL,
  `group_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `group_participants`
--

INSERT INTO `group_participants` (`id`, `company_id`, `quantity`, `group_id`) VALUES
(10, 1, 1, 10),
(11, 1, 1, 11),
(12, 5, 5, 13),
(14, 2, 1, 14),
(15, 1, 1, 14),
(18, 5, 900, 16),
(19, 6, 800, 16),
(21, 3, 100, 16);

-- --------------------------------------------------------

--
-- Structure de la table `group_purchases`
--

CREATE TABLE `group_purchases` (
  `id` bigint(20) NOT NULL,
  `current_quantity` int(11) NOT NULL,
  `deadline` datetime(6) NOT NULL,
  `status` enum('OPEN','FULL','SUCCESS','FAILED','CLOSED') NOT NULL,
  `target_quantity` int(11) NOT NULL,
  `listing_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `group_purchases`
--

INSERT INTO `group_purchases` (`id`, `current_quantity`, `deadline`, `status`, `target_quantity`, `listing_id`) VALUES
(10, 2, '2026-04-16 16:42:23.000000', 'OPEN', 5, 15),
(11, 1, '2026-04-14 16:42:23.000000', 'OPEN', 10, 16),
(12, 0, '2026-04-10 17:07:00.000000', 'OPEN', 5000, 17),
(13, 5, '2026-05-02 15:46:00.000000', 'FULL', 5, 25),
(14, 2, '2026-04-25 20:05:00.000000', 'FULL', 2, 26),
(16, 1800, '2026-05-10 19:50:26.000000', 'OPEN', 5000, 34);

-- --------------------------------------------------------

--
-- Structure de la table `inventory_scan`
--

CREATE TABLE `inventory_scan` (
  `id` bigint(20) NOT NULL,
  `barcode` varchar(255) DEFAULT NULL,
  `real_condition` varchar(255) DEFAULT NULL,
  `real_location` varchar(255) DEFAULT NULL,
  `real_qty` int(11) NOT NULL,
  `scanned_at` datetime(6) DEFAULT NULL,
  `id_product` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `invoice`
--

CREATE TABLE `invoice` (
  `id` bigint(20) NOT NULL,
  `amount_ht` double NOT NULL,
  `amount_ttc` double NOT NULL,
  `buyer_article_fiscal` varchar(50) DEFAULT NULL,
  `client_name` varchar(255) NOT NULL,
  `delivered_at` varchar(255) DEFAULT NULL,
  `delivery_order_id` bigint(20) DEFAULT NULL,
  `id_stock` bigint(20) DEFAULT NULL,
  `invoice_number` varchar(255) NOT NULL,
  `invoice_type` enum('VENTE','ACHAT') DEFAULT NULL,
  `issue_date` varchar(255) DEFAULT NULL,
  `linked_escrow_id` bigint(20) DEFAULT NULL,
  `project` varchar(255) NOT NULL,
  `seller_article_fiscal` varchar(50) DEFAULT NULL,
  `seller_name` varchar(255) DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  `tva` double NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `invoice`
--

INSERT INTO `invoice` (`id`, `amount_ht`, `amount_ttc`, `buyer_article_fiscal`, `client_name`, `delivered_at`, `delivery_order_id`, `id_stock`, `invoice_number`, `invoice_type`, `issue_date`, `linked_escrow_id`, `project`, `seller_article_fiscal`, `seller_name`, `status`, `tva`) VALUES
(8, 5000, 5950, NULL, 'izzz', '2026-04-23', 70, NULL, 'INV-811989', NULL, '2026-04-13', 8, 'aaa', NULL, NULL, 'PAID', 19),
(9, 100, 119, '5454', 'aaa', '2026-04-25', 10, NULL, 'INV-188097', 'VENTE', '2026-04-23', 9, 'qas', '1321', 'sss', 'PAID', 19),
(10, 200, 238, '5135', 'qfqsdf', '2026-04-23', 42, NULL, 'INV-787082', NULL, '2026-04-23', 11, 'sqqsssd', '156', 'sdsdB2B', 'PAID', 19),
(11, 100, 119, '656', 'skjdihi', '2026-04-23', 45, NULL, 'INV-580178', NULL, '2026-04-23', 12, 'skmkm', '6565', 'EcoRessource B2B', 'PAID', 19),
(12, 200, 238, '54354', 'scs', '2026-04-23', 70, NULL, 'INV-149452', NULL, '2026-04-23', 13, 'csfyy', '3232', 'scB2B', 'PAID', 19),
(14, 120, 142.8, '6564', 'dqlji', NULL, 80, NULL, 'INV-539598', NULL, '2025-01-23', 16, 'sirine', '65565', 'EcoRessource B2B', 'UNPAID', 19),
(15, 400, 476, '5545646', 'sidhu', NULL, 546, NULL, 'INV-845745', NULL, '2026-04-25', 17, 'linda ', '566564', 'EcoRessource B2B', 'UNPAID', 19),
(16, 400, 476, 'lkjkj', 'bkhk', '2026-04-25', 133, NULL, 'INV-448235', NULL, '2026-04-25', 18, 'hjde', 'mmlklj', 'EcoRessource B2B', 'PAID', 19),
(17, 422, 502.18, '8951', 'aaa', NULL, 70, NULL, 'INV-099695', NULL, '2025-01-01', 19, 'qqq', '326', 'qsd', 'UNPAID', 19),
(18, 700, 833, '5684684', 'lmm', NULL, 78, NULL, 'INV-448247', NULL, '2026-04-25', 20, 'dsss', '4544', 'EcoRessource B2B', 'UNPAID', 19),
(19, 400, 476, '6465', 'aaa', NULL, 80, NULL, 'INV-929420', 'VENTE', '2026-04-25', 21, 'ddh', '654564', 'EcoRessource B2B', 'UNPAID', 19),
(20, 400, 476, '313351', 'qss', NULL, 660, NULL, 'INV-305203', 'VENTE', '2026-04-25', 23, 'qqqq', '45654', 'EcoRessource B2B', 'UNPAID', 19),
(21, 100, 119, '654646', 'sss', '2026-04-25', 54, NULL, 'ACH-2026-001', 'ACHAT', '2026-04-25', 25, 'hggiu', '3454', 'sss', 'PAID', 19),
(22, 700, 833, '54654', 'llm', '2026-04-26', 456, NULL, 'VTE-2026-004', 'VENTE', '2026-04-25', 26, 'abc', '643643', 'sss', 'PAID', 19),
(23, 100, 119, '565545', 'sxsds', '2026-04-27', 5654, NULL, 'VTE-2026-005', 'VENTE', '2026-04-26', 28, 'qdqd', '5465464', 'sss', 'PAID', 19),
(24, 100, 109, '2365', 'sss', '2026-04-28', 444, NULL, 'ACH-2026-002', 'ACHAT', '2026-04-28', 30, 'sisi', 'TN123', 'Industrie Slim SARL', 'PAID', 9),
(25, 900, 1071, '23655', 'aaaa', '2026-04-28', 888, NULL, 'VTE-2026-006', 'VENTE', '2026-04-28', 31, 'izzz', '35354', 'sss', 'PAID', 19);

-- --------------------------------------------------------

--
-- Structure de la table `listings`
--

CREATE TABLE `listings` (
  `id` bigint(20) NOT NULL,
  `ai_insight` varchar(255) DEFAULT NULL,
  `category` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `enquiries` int(11) DEFAULT NULL,
  `posted_label` varchar(255) DEFAULT NULL,
  `price` decimal(14,2) NOT NULL,
  `quantity_label` varchar(255) NOT NULL,
  `status` enum('active','pending','draft','rejected') NOT NULL,
  `title` varchar(255) NOT NULL,
  `views` int(11) DEFAULT NULL,
  `enterprise_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `listings`
--

INSERT INTO `listings` (`id`, `ai_insight`, `category`, `created_at`, `enquiries`, `posted_label`, `price`, `quantity_label`, `status`, `title`, `views`, `enterprise_id`) VALUES
(1, 'High demand — act fast', 'Metal', '2026-04-27 19:58:19.000000', 5, 'Mar 1', 1200.00, '2,000 kg', 'active', 'Aluminum Scrap 2T', 48, 7);

-- --------------------------------------------------------

--
-- Structure de la table `platform_events`
--

CREATE TABLE `platform_events` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `event_date` date NOT NULL,
  `location` varchar(255) NOT NULL,
  `participants` int(11) NOT NULL,
  `status` enum('upcoming','ongoing','done') NOT NULL,
  `title` varchar(255) NOT NULL,
  `type_label` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `platform_events`
--

INSERT INTO `platform_events` (`id`, `created_at`, `event_date`, `location`, `participants`, `status`, `title`, `type_label`) VALUES
(1, '2026-04-09 15:23:41.000000', '2025-04-10', 'Tunis', 42, 'upcoming', 'B2B Industrial Fair 2025', 'Conference');

-- --------------------------------------------------------

--
-- Structure de la table `post_attachments`
--

CREATE TABLE `post_attachments` (
  `id` bigint(20) NOT NULL,
  `file_url` varchar(255) NOT NULL,
  `listing_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `post_attachments`
--

INSERT INTO `post_attachments` (`id`, `file_url`, `listing_id`) VALUES
(10, 'uploads/aluminum1.jpg', 10),
(11, 'uploads/aluminum2.jpg', 10),
(12, 'uploads/plastic.jpg', 11),
(13, 'uploads/glass.jpg', 12),
(15, 'uploads/paper.jpg', 14),
(16, 'uploads/cnc.jpg', 15),
(17, 'uploads/aluminum_group.jpg', 16),
(20, 'uploads/wood.jpg', 13),
(28, 'uploads/aluminum_group.jpg', 18),
(29, '/files/1776868194796_5e81bcbd.png', 21),
(32, '/files/1776868275358_280db26c.png', 22),
(33, '/files/1776868837060_ea31410e.png', 22),
(34, '/files/1776868841703_0f52d2e5.jpg', 22),
(35, '/files/1776868275358_280db26c.png', 23),
(36, '/files/1776868837060_ea31410e.png', 23),
(37, '/files/1776868841703_0f52d2e5.jpg', 23),
(38, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTndc60D3vyCFZIbNLbynSmxtgRWYRlqOBMCQ&s', 24),
(39, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTndc60D3vyCFZIbNLbynSmxtgRWYRlqOBMCQ&s', 25),
(40, '/files/1776888347717_c9521fa4.jpg', 26),
(41, '/files/1776888352523_b83c1f89.jpg', 26),
(49, 'uploads/aluminum2.jpg', 32),
(50, 'uploads/aluminum2.jpg', 33),
(51, 'uploads/aluminum2.jpg', 34),
(52, 'uploads/aluminum2.jpg', 35),
(53, 'uploads/aluminum2.jpg', 36),
(54, 'uploads/aluminum2.jpg', 37),
(56, '/files/1777395179081_13a9c6a8.jpg', 92),
(57, '/files/1777471088320_1dd51bb3.jpg', 93);

-- --------------------------------------------------------

--
-- Structure de la table `product`
--

CREATE TABLE `product` (
  `id_product` bigint(20) NOT NULL,
  `category` varchar(255) DEFAULT NULL,
  `company_id` bigint(20) DEFAULT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `image` varchar(255) DEFAULT NULL,
  `material_type` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `recyclable` bit(1) NOT NULL,
  `barcode` varchar(255) DEFAULT NULL,
  `enterprise_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `product`
--

INSERT INTO `product` (`id_product`, `category`, `company_id`, `description`, `image`, `material_type`, `name`, `recyclable`, `barcode`, `enterprise_id`) VALUES
(2, 'Plastic', NULL, 'csfvgbgbfgbffhnhn', '1777384096814_04adb472.png', 'Raw', 'ddcqdccs', b'0', '2000000000002', 4),
(3, 'electronic', NULL, 'azertyuiopqsdfghjk', '1777387797238_c0b5a42c.jpg', 'metal', 'iphone', b'0', NULL, NULL),
(10, 'Metal', 1, 'High quality aluminum scrap', 'aluminum.jpg', 'Aluminum', 'Aluminum Scrap', b'1', NULL, NULL),
(11, 'Plastic', 1, 'Recycled plastic pellets', 'plastic.jpg', 'Plastic', 'Plastic Pellets', b'1', NULL, NULL),
(12, 'Glass', 1, 'Recycled glass materials', 'glass.jpg', 'Glass', 'Glass Waste', b'1', NULL, NULL),
(13, 'Machine', 1, 'Industrial CNC Machine', 'cnc.jpg', 'Steel', 'CNC Machine', b'0', NULL, NULL),
(14, 'Wood', 1, 'Wood pallets reusable', 'wood.jpg', 'Wood', 'Wood Pallets', b'1', NULL, NULL),
(15, 'Paper', 1, 'Paper waste for recycling', 'paper.jpg', 'Paper', 'Paper Waste', b'1', NULL, NULL),
(16, 'Metal', NULL, 'Lot industriel d aluminium recyclable, propre et pret pour transformation.', 'uploads/aluminum2.jpg', 'Aluminium', '[TEST] Aluminium recycle premium', b'1', 'TEST-ALU-001', 1),
(17, 'Plastique', NULL, 'Granules PET recyclees pour injection et extrusion.', 'uploads/aluminum2.jpg', 'PET', '[TEST] Granules plastique PET', b'1', 'TEST-PET-001', 5),
(18, 'Textile', NULL, 'Chutes textiles propres issues de production industrielle.', 'uploads/aluminum2.jpg', 'Coton', '[TEST] Chutes textile coton', b'1', 'TEST-TEXT-001', 4),
(19, 'Verre', NULL, 'Verre transparent trie et concasse pour recyclage.', 'uploads/aluminum2.jpg', 'Verre', '[TEST] Verre transparent concasse', b'1', 'TEST-VERRE-001', 6),
(20, 'Metal', NULL, 'Industrial aluminum scrap for recycling', 'aluminum-scrap.jpg', 'Aluminum', 'Aluminum Scrap', b'1', NULL, 7);

-- --------------------------------------------------------

--
-- Structure de la table `reclamations`
--

CREATE TABLE `reclamations` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `damaged_quantity` int(11) DEFAULT NULL,
  `damaged_unit` varchar(20) DEFAULT NULL,
  `defect_type` varchar(100) DEFAULT NULL,
  `description` varchar(500) NOT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `resolution_notes` text DEFAULT NULL,
  `resolved_at` datetime(6) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  `enterprise_id` bigint(20) NOT NULL,
  `product_id` bigint(20) DEFAULT NULL,
  `stock_item_id` bigint(20) DEFAULT NULL,
  `target_enterprise_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `reclamations`
--

INSERT INTO `reclamations` (`id`, `created_at`, `damaged_quantity`, `damaged_unit`, `defect_type`, `description`, `image_url`, `resolution_notes`, `resolved_at`, `status`, `enterprise_id`, `product_id`, `stock_item_id`, `target_enterprise_id`) VALUES
(1, '2026-04-28 14:31:52.000000', 1, NULL, 'Cracked / Broken', 'Screen is cracked / broken', '1777386711685_3e42a87d.jpg', 'Defect classification: The defect claim for the \'ddcqdccs\' product, specifically the \'Cracked / Broken\' screen, has been confirmed. Given that the product is made of \'Raw Plastic\' material in a \'Good\' condition at the time of stock entry, I assess the severity as moderate to high due to the potential for further damage or leakage. Based on the 78 kg quantity in stock and unit price of 7.0 TND, the estimated financial impact is approximately 540 TND. \n\nRecommendation: I recommend that the affected stock be removed from inventory and quarantined for disposal, and an investigation be conducted to determine the root cause of the defect to prevent future occurrences.\n\nApproved by stock owner. Stock decreased by 1. New quantity: 77.', '2026-04-28 14:32:32.000000', 'TREATED', 4, 2, 2, 4);

-- --------------------------------------------------------

--
-- Structure de la table `reservations`
--

CREATE TABLE `reservations` (
  `id` bigint(20) NOT NULL,
  `company_name` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `from_date` date NOT NULL,
  `item` varchar(255) NOT NULL,
  `price` decimal(14,2) NOT NULL,
  `status` enum('confirmed','active','pending','completed') NOT NULL,
  `to_date` date NOT NULL,
  `type_label` varchar(255) NOT NULL,
  `enterprise_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `reservations`
--

INSERT INTO `reservations` (`id`, `company_name`, `created_at`, `from_date`, `item`, `price`, `status`, `to_date`, `type_label`, `enterprise_id`) VALUES
(1, 'Industrie Slim', '2026-04-27 19:58:19.000000', '2025-03-20', 'CNC Milling 3-axis', 450.00, 'confirmed', '2025-03-22', 'Machine', 7);

-- --------------------------------------------------------

--
-- Structure de la table `resource_listings`
--

CREATE TABLE `resource_listings` (
  `id` bigint(20) NOT NULL,
  `company_id` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(2000) NOT NULL,
  `latitude` double DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `price` decimal(14,2) DEFAULT NULL,
  `quantity` int(11) NOT NULL,
  `status` enum('ACTIVE','CLOSED','EXPIRED','CANCELLED') NOT NULL,
  `title` varchar(255) NOT NULL,
  `type` enum('SURPLUS','DEMANDE','GROUP_BUYING') NOT NULL,
  `unit` varchar(255) NOT NULL,
  `product_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `resource_listings`
--

INSERT INTO `resource_listings` (`id`, `company_id`, `created_at`, `description`, `latitude`, `location`, `longitude`, `price`, `quantity`, `status`, `title`, `type`, `unit`, `product_id`) VALUES
(10, 2, '2026-04-09 16:42:23.000000', 'Aluminum scrap available in bulk', 36.8, 'Tunis', 10.18, 1200.00, 3000, 'ACTIVE', 'Aluminum Scrap Bulk', 'SURPLUS', 'kg', 10),
(11, 2, '2026-04-09 16:42:23.000000', 'Plastic pellets ready for reuse', 34.7, 'Sfax', 10.76, 800.00, 1500, 'ACTIVE', 'Plastic Pellets Stock', 'SURPLUS', 'kg', 11),
(12, 2, '2026-04-09 16:42:23.000000', 'Glass waste for recycling industries', 35.8, 'Sousse', 10.64, 500.00, 2000, 'ACTIVE', 'Glass Waste Stock', 'SURPLUS', 'kg', 12),
(13, 2, '2026-04-09 16:42:23.000000', 'Looking for wood pallgggggggets urgently', 36.8, 'Tunis', 10.18, 300.00, 500, 'ACTIVE', 'Need Wood Pallets', 'DEMANDE', 'unit', 10),
(14, 2, '2026-04-09 16:42:23.000000', 'Need paper waste suppliers', 34.7, 'Sfax', 10.76, 200.00, 1000, 'ACTIVE', 'Paper Waste Needed', 'DEMANDE', 'kg', 15),
(15, 2, '2026-04-09 16:42:23.000000', 'Group purchase for CNC machine', 36.8, 'Tunis', 10.18, 5000.00, 1, 'ACTIVE', 'CNC Machine Group Buy', 'GROUP_BUYING', 'unit', 13),
(16, 2, '2026-04-09 16:42:23.000000', 'Bulk aluminum group purchase', 36.8, 'Tunis', 10.18, 900.00, 5000, 'ACTIVE', 'Aluminum Group Deal', 'GROUP_BUYING', 'kg', 10),
(17, 2, '2026-04-09 17:07:45.000000', 'testtttttttttt200', NULL, 'Sousse, Tunisie', NULL, 29999.00, 2000, 'ACTIVE', 'Plastic Pellets Stock', 'GROUP_BUYING', '10', 13),
(18, 2, '2026-04-22 12:49:41.000000', 'teeaaaaaaaaaaaaaaaaeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee', 36.8, 'Tunis', 10.18, 8000.00, 7000, 'ACTIVE', 'Aluminum Group Deal (copie)', 'GROUP_BUYING', 'kg', 10),
(19, 2, '2026-04-22 12:51:04.000000', 'gggggggggggggggggggggg', NULL, 'Monastir, Tunisie', NULL, 20.00, 3000, 'ACTIVE', 'Surplus Textile', 'SURPLUS', 'KG', 14),
(20, 2, '2026-04-22 13:04:50.000000', 'gfffffffffffffffffffffffffffff', NULL, 'Sfax', NULL, 40000.00, 277, 'CANCELLED', 'fggggggggggggggggggggggg', 'SURPLUS', 'KG', 13),
(21, 2, '2026-04-22 14:30:01.000000', 'QQQQQQQQQQQQQQQQQQQQQQ', NULL, 'SFAX', NULL, 0.07, 2000, 'ACTIVE', 'Paper Waste Needed', 'SURPLUS', 'KG', 13),
(22, 2, '2026-04-22 14:31:01.000000', 'wwwwwwwwww', 0, 'Ariana', 0, 0.03, 1000, 'ACTIVE', 'wwwwwwwwwwww', 'SURPLUS', 'KG', 12),
(23, 2, '2026-04-22 15:43:24.000000', 'wwwwwwwwww', 0, 'Ariana', 0, 0.03, 1000, 'ACTIVE', 'wwwwwwwwwwww (copie)', 'SURPLUS', 'KG', 12),
(24, 2, '2026-04-22 15:45:22.000000', 'a la recherche ', NULL, 'Tunis, Sfax', NULL, 0.01, 20000, 'CANCELLED', 'Need Wood Pallets', 'DEMANDE', 'kg', 10),
(25, 2, '2026-04-22 15:46:36.000000', 'dgfgfgfgDEDR', NULL, 'Nabeul', NULL, 222222222222.00, 20000000, 'ACTIVE', 'sfdfgfdgd', 'GROUP_BUYING', 'kg', 12),
(26, 5, '2026-04-22 20:05:56.000000', 'lotttttttttttttt', NULL, 'Tozeur', NULL, 1000.00, 3000, 'ACTIVE', 'lottt', 'GROUP_BUYING', 'kg', 11),
(32, 1, '2026-04-20 19:50:26.000000', 'Lot d aluminium recyclable propre, conditionne en sacs industriels. Disponible immediatement pour transformateurs et recycleurs. Ideal pour production locale avec volume stable.', 36.8065, 'Tunis, Tunisie', 10.1815, 3.40, 2200, 'ACTIVE', 'Surplus aluminium propre - Tunis', 'SURPLUS', 'kg', 16),
(33, 5, '2026-04-21 19:50:26.000000', 'Entreprise cherche aluminium recycle de bonne qualite pour production industrielle. Livraison souhaitee sous 10 jours avec prix negocie selon volume.', 34.7406, 'Sfax, Tunisie', 10.7603, 3.75, 1200, 'ACTIVE', 'Urgente aluminium - Sfax', 'DEMANDE', 'kg', 16),
(34, 4, '2026-04-22 19:50:26.000000', 'Achat groupe de granules PET recyclees. Plus le volume est atteint rapidement, plus le cout logistique devient interessant pour les participants.', 35.8256, 'Sousse, Tunisie', 10.6369, 1.30, 5000, 'ACTIVE', 'Achat groupe PET recycle - Sousse', 'GROUP_BUYING', 'kg', 17),
(35, 4, '2026-04-23 19:50:26.000000', 'Chutes de coton propres, separees par couleur claire. Convient pour rembourrage, recyclage textile ou valorisation artisanale.', 35.777, 'Monastir, Tunisie', 10.8262, 0.95, 850, 'ACTIVE', 'Surplus chutes textile coton - Monastir', 'SURPLUS', 'kg', 18),
(36, 6, '2026-04-24 19:50:26.000000', 'Verre transparent concasse, trie et pret pour recyclage. Lot regulier disponible avec possibilite de contrat mensuel.', 37.2744, 'Bizerte, Tunisie', 9.8739, 0.60, 3000, 'ACTIVE', 'Verre transparent concasse - Bizerte', 'SURPLUS', 'kg', 19),
(37, 1, '2026-04-25 19:50:26.000000', 'Recherche textile recycle propre pour fabrication de panneaux isolants. Priorite aux lots homogenes et deja tries.', 36.8065, 'Tunis, Tunisie', 10.1815, 1.10, 600, 'ACTIVE', 'Demande textile recycle - Tunis', 'DEMANDE', 'kg', 18),
(38, 1, '2026-04-17 19:50:26.000000', 'Annonce de test annulee pour verifier les statuts et les actions desactivees cote frontend.', 36.8665, 'Ariana, Tunisie', 10.1647, 3.10, 700, 'CANCELLED', 'Ancienne annonce aluminium annulee', 'SURPLUS', 'kg', 16),
(39, 1, '2026-04-19 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.8202890084314, 'Tunis, Tunisie', 10.127786276235977, 3.74, 3191, 'ACTIVE', '[TEST] Surplus auto aluminium 1', 'SURPLUS', 'kg', 16),
(40, 1, '2026-04-17 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.88221995558796, 'Tunis, Tunisie', 10.109234465015339, 2.27, 708, 'ACTIVE', '[TEST] Surplus auto aluminium 2', 'SURPLUS', 'kg', 16),
(42, 1, '2026-04-26 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.82916399876509, 'Tunis, Tunisie', 10.198498286398591, 2.01, 1997, 'ACTIVE', '[TEST] Surplus auto aluminium 4', 'SURPLUS', 'kg', 16),
(43, 1, '2026-04-22 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.87405986699653, 'Tunis, Tunisie', 10.172759211783072, 2.89, 1385, 'ACTIVE', '[TEST] Surplus auto aluminium 5', 'SURPLUS', 'kg', 16),
(44, 1, '2026-04-20 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.851455903287466, 'Tunis, Tunisie', 10.18475756848674, 3.16, 3194, 'ACTIVE', '[TEST] Surplus auto aluminium 6', 'SURPLUS', 'kg', 16),
(45, 1, '2026-04-18 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.800913452171635, 'Tunis, Tunisie', 10.136928837967039, 3.61, 3284, 'ACTIVE', '[TEST] Surplus auto aluminium 7', 'SURPLUS', 'kg', 16),
(46, 1, '2026-04-23 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.843561237625366, 'Tunis, Tunisie', 10.173950642416207, 3.29, 3461, 'ACTIVE', '[TEST] Surplus auto aluminium 8', 'SURPLUS', 'kg', 16),
(47, 1, '2026-04-24 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.8308604030226, 'Tunis, Tunisie', 10.103493721227622, 3.46, 2704, 'ACTIVE', '[TEST] Surplus auto aluminium 9', 'SURPLUS', 'kg', 16),
(49, 1, '2026-04-24 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.834236402282706, 'Tunis, Tunisie', 10.124847724311843, 4.70, 1263, 'ACTIVE', '[TEST] Surplus auto aluminium 11', 'SURPLUS', 'kg', 16),
(50, 1, '2026-04-26 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.805344970994945, 'Tunis, Tunisie', 10.123930822987045, 1.55, 1493, 'ACTIVE', '[TEST] Surplus auto aluminium 12', 'SURPLUS', 'kg', 16),
(52, 1, '2026-04-19 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.87678892405404, 'Tunis, Tunisie', 10.141568899565906, 3.37, 2105, 'ACTIVE', '[TEST] Surplus auto aluminium 14', 'SURPLUS', 'kg', 16),
(53, 1, '2026-04-21 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.81683694507632, 'Tunis, Tunisie', 10.14125505671022, 4.46, 2380, 'ACTIVE', '[TEST] Surplus auto aluminium 15', 'SURPLUS', 'kg', 16),
(54, 1, '2026-04-24 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.87478922575227, 'Tunis, Tunisie', 10.14996983637099, 2.03, 2151, 'ACTIVE', '[TEST] Surplus auto aluminium 16', 'SURPLUS', 'kg', 16),
(55, 1, '2026-04-23 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.82273411436261, 'Tunis, Tunisie', 10.180533649940541, 2.72, 2824, 'ACTIVE', '[TEST] Surplus auto aluminium 17', 'SURPLUS', 'kg', 16),
(56, 1, '2026-04-22 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.85904061036095, 'Tunis, Tunisie', 10.144467207458305, 3.58, 1421, 'ACTIVE', '[TEST] Surplus auto aluminium 18', 'SURPLUS', 'kg', 16),
(57, 1, '2026-04-25 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.86051427559975, 'Tunis, Tunisie', 10.119457848760726, 2.52, 3280, 'ACTIVE', '[TEST] Surplus auto aluminium 19', 'SURPLUS', 'kg', 16),
(58, 1, '2026-04-23 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.81169197523192, 'Tunis, Tunisie', 10.194799293014034, 3.36, 1110, 'ACTIVE', '[TEST] Surplus auto aluminium 20', 'SURPLUS', 'kg', 16),
(59, 1, '2026-04-18 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.840698279664565, 'Tunis, Tunisie', 10.100703730155438, 3.05, 806, 'ACTIVE', '[TEST] Surplus auto aluminium 21', 'SURPLUS', 'kg', 16),
(60, 1, '2026-04-22 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.88881630719529, 'Tunis, Tunisie', 10.101777560172396, 4.86, 650, 'ACTIVE', '[TEST] Surplus auto aluminium 22', 'SURPLUS', 'kg', 16),
(62, 1, '2026-04-21 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.82067450128558, 'Tunis, Tunisie', 10.143506151012616, 5.09, 624, 'ACTIVE', '[TEST] Surplus auto aluminium 24', 'SURPLUS', 'kg', 16),
(64, 1, '2026-04-24 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.80643176437023, 'Tunis, Tunisie', 10.169160324399508, 4.43, 1818, 'ACTIVE', '[TEST] Surplus auto aluminium 26', 'SURPLUS', 'kg', 16),
(66, 1, '2026-04-25 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.87939760366398, 'Tunis, Tunisie', 10.130646392079672, 3.87, 2478, 'ACTIVE', '[TEST] Surplus auto aluminium 28', 'SURPLUS', 'kg', 16),
(67, 1, '2026-04-20 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.806057694373706, 'Tunis, Tunisie', 10.11679199954196, 3.81, 2997, 'ACTIVE', '[TEST] Surplus auto aluminium 29', 'SURPLUS', 'kg', 16),
(68, 1, '2026-04-18 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.841485294179506, 'Tunis, Tunisie', 10.12112983234332, 5.15, 2856, 'ACTIVE', '[TEST] Surplus auto aluminium 30', 'SURPLUS', 'kg', 16),
(69, 1, '2026-04-19 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.81879296528063, 'Tunis, Tunisie', 10.186050601011189, 3.97, 1777, 'ACTIVE', '[TEST] Surplus auto aluminium 31', 'SURPLUS', 'kg', 16),
(70, 1, '2026-04-23 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.83870108662052, 'Tunis, Tunisie', 10.190091015389273, 3.10, 836, 'ACTIVE', '[TEST] Surplus auto aluminium 32', 'SURPLUS', 'kg', 16),
(71, 1, '2026-04-25 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.81741598538814, 'Tunis, Tunisie', 10.173955436771694, 2.31, 544, 'ACTIVE', '[TEST] Surplus auto aluminium 33', 'SURPLUS', 'kg', 16),
(73, 1, '2026-04-24 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.88973782462043, 'Tunis, Tunisie', 10.159348548910906, 4.88, 2424, 'ACTIVE', '[TEST] Surplus auto aluminium 35', 'SURPLUS', 'kg', 16),
(74, 1, '2026-04-23 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.89827686576012, 'Tunis, Tunisie', 10.14513690894948, 2.29, 2288, 'ACTIVE', '[TEST] Surplus auto aluminium 36', 'SURPLUS', 'kg', 16),
(75, 1, '2026-04-25 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.85208939672642, 'Tunis, Tunisie', 10.15524709844519, 1.72, 1065, 'ACTIVE', '[TEST] Surplus auto aluminium 37', 'SURPLUS', 'kg', 16),
(76, 1, '2026-04-21 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.850585575076366, 'Tunis, Tunisie', 10.121205115244914, 2.69, 1522, 'ACTIVE', '[TEST] Surplus auto aluminium 38', 'SURPLUS', 'kg', 16),
(77, 1, '2026-04-17 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.85600361642987, 'Tunis, Tunisie', 10.152503847379707, 4.62, 731, 'ACTIVE', '[TEST] Surplus auto aluminium 39', 'SURPLUS', 'kg', 16),
(78, 1, '2026-04-25 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.81304327381127, 'Tunis, Tunisie', 10.190335663864701, 4.85, 950, 'ACTIVE', '[TEST] Surplus auto aluminium 40', 'SURPLUS', 'kg', 16),
(79, 1, '2026-04-22 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.829953862475186, 'Tunis, Tunisie', 10.186673070477948, 2.62, 3252, 'ACTIVE', '[TEST] Surplus auto aluminium 41', 'SURPLUS', 'kg', 16),
(80, 1, '2026-04-25 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.81243539807613, 'Tunis, Tunisie', 10.19121641916336, 3.85, 2224, 'ACTIVE', '[TEST] Surplus auto aluminium 42', 'SURPLUS', 'kg', 16),
(81, 1, '2026-04-24 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.863427364140215, 'Tunis, Tunisie', 10.182665923128525, 3.59, 1106, 'ACTIVE', '[TEST] Surplus auto aluminium 43', 'SURPLUS', 'kg', 16),
(82, 1, '2026-04-23 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.83356415762898, 'Tunis, Tunisie', 10.166650541188801, 4.32, 2517, 'ACTIVE', '[TEST] Surplus auto aluminium 44', 'SURPLUS', 'kg', 16),
(83, 1, '2026-04-24 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.894286799984314, 'Tunis, Tunisie', 10.121732716561977, 2.80, 2385, 'ACTIVE', '[TEST] Surplus auto aluminium 45', 'SURPLUS', 'kg', 16),
(84, 1, '2026-04-22 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.816943456341455, 'Tunis, Tunisie', 10.159679203349798, 3.32, 2414, 'ACTIVE', '[TEST] Surplus auto aluminium 46', 'SURPLUS', 'kg', 16),
(85, 1, '2026-04-18 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.879909490775226, 'Tunis, Tunisie', 10.145778599703478, 3.49, 2263, 'ACTIVE', '[TEST] Surplus auto aluminium 47', 'SURPLUS', 'kg', 16),
(86, 1, '2026-04-21 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.84924278580513, 'Tunis, Tunisie', 10.121391931941167, 4.30, 754, 'ACTIVE', '[TEST] Surplus auto aluminium 48', 'SURPLUS', 'kg', 16),
(88, 1, '2026-04-24 19:51:54.000000', 'Lot genere automatiquement pour test de charge et pagination.', 36.81622339302322, 'Tunis, Tunisie', 10.134675487442571, 3.33, 778, 'ACTIVE', '[TEST] Surplus auto aluminium 50', 'SURPLUS', 'kg', 16),
(89, 1, '2026-04-26 19:52:06.000000', 'Test geo', 33.8815, 'Gabes', 10.0982, 3.20, 1000, 'ACTIVE', '[TEST] Aluminium Gabes', 'SURPLUS', 'kg', 16),
(91, 1, '2026-04-26 19:52:06.000000', 'Test geo', 36.451, 'Nabeul', 10.735, 3.50, 1200, 'ACTIVE', '[TEST] Aluminium Nabeul', 'SURPLUS', 'kg', 16),
(92, 1, '2026-04-28 16:53:02.000000', 'Contactez-nous pour confirmer la disponibilite, demander des photos supplementaires ou proposer une offre.\n\nProduit: Plastic Pellets. Quantite disponible: a preciser .\n\nEtat, qualite et conditions de stockage a preciser pour rassurer les acheteurs professionnels.\n\nEnlevement ou livraison a organiser. Delai et modalites negociables.\n\nContactez-nous pour confirmer la disponibilite, demander des photos supplementaires ou proposer une offre.', 36.940321, 'Ariana, Tunisia', 10.130663, NULL, 5000, 'CANCELLED', 'lot aluminium', 'SURPLUS', 'pièces', 11),
(93, 1, '2026-04-29 13:58:10.000000', 'Annonce professionnelle pour [TEST] Aluminium recycle premium (Metal). Quantite disponible/recherchee: 2000 pièces. Localisation: Djerba, Paris, France. Produit [TEST] Aluminium recycle premium trie et pret pour un usage B2B. Categorie Metal avec informations claires sur volume et unite. Retrait ou coordination logistique possible autour de Djerba, Paris, France. Prix indicatif: 3.45679170435E9 TND. Contactez-nous avec votre disponibilite, delai et conditions de livraison.', 48.884137, 'Djerba, Paris, France', 2.351781, 5.62, 2000, 'CANCELLED', 'Recherche [TEST] Aluminium Recycle Premium - 2000 Pièces A Djerba, Paris, France', 'DEMANDE', 'pièces', 16);

-- --------------------------------------------------------

--
-- Structure de la table `shipments`
--

CREATE TABLE `shipments` (
  `id` bigint(20) NOT NULL,
  `date_depart` datetime(6) NOT NULL,
  `id_transporter` bigint(20) NOT NULL,
  `produit_id` bigint(20) NOT NULL,
  `quantite` double NOT NULL,
  `statut` enum('EN_ATTENTE','EN_COURS','LIVREE') NOT NULL,
  `delivery_order_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `shipments`
--

INSERT INTO `shipments` (`id`, `date_depart`, `id_transporter`, `produit_id`, `quantite`, `statut`, `delivery_order_id`) VALUES
(9, '2026-04-29 09:40:41.000000', 1, 1, 1, 'LIVREE', 11),
(10, '2026-04-29 09:47:55.000000', 1, 1, 1, 'LIVREE', 12),
(11, '2026-04-29 09:55:18.000000', 1, 1, 1, 'LIVREE', 13);

-- --------------------------------------------------------

--
-- Structure de la table `solidarity_associations`
--

CREATE TABLE `solidarity_associations` (
  `id` bigint(20) NOT NULL,
  `ai_insight` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `donations` int(11) NOT NULL,
  `members` int(11) NOT NULL,
  `mission` varchar(2000) NOT NULL,
  `name` varchar(255) NOT NULL,
  `status_label` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `solidarity_associations`
--

INSERT INTO `solidarity_associations` (`id`, `ai_insight`, `created_at`, `donations`, `members`, `mission`, `name`, `status_label`) VALUES
(1, 'High donation success rate', '2026-04-09 15:23:41.000000', 4500, 120, 'Household waste sorting & recycling awareness', 'Recycly Tunisia', 'active');

-- --------------------------------------------------------

--
-- Structure de la table `stock_item`
--

CREATE TABLE `stock_item` (
  `id_stock` bigint(20) NOT NULL,
  `company_id` bigint(20) DEFAULT NULL,
  `expiration_date` date DEFAULT NULL,
  `image` varchar(255) DEFAULT NULL,
  `item_condition` varchar(255) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `quantity` int(11) NOT NULL,
  `status` varchar(255) DEFAULT NULL,
  `unit` varchar(255) DEFAULT NULL,
  `unit_price` double NOT NULL,
  `id_product` bigint(20) DEFAULT NULL,
  `deleted` bit(1) NOT NULL,
  `enterprise_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `stock_item`
--

INSERT INTO `stock_item` (`id_stock`, `company_id`, `expiration_date`, `image`, `item_condition`, `location`, `quantity`, `status`, `unit`, `unit_price`, `id_product`, `deleted`, `enterprise_id`) VALUES
(1, 1, '2026-10-23', 'uploads/aluminum2.jpg', 'Excellent', 'Tunis', 2500, 'AVAILABLE', 'kg', 3.2, 16, b'0', 1),
(2, 5, '2026-08-24', 'uploads/aluminum2.jpg', 'Bon', 'Sfax', 1600, 'AVAILABLE', 'kg', 1.4, 17, b'0', 5),
(3, 4, '2026-07-25', 'uploads/aluminum2.jpg', 'Trie', 'Sousse', 900, 'AVAILABLE', 'kg', 0.85, 18, b'0', 4),
(4, 6, '2026-12-22', 'uploads/aluminum2.jpg', 'Concasse', 'Bizerte', 3200, 'AVAILABLE', 'kg', 0.55, 19, b'0', 6),
(5, 7, NULL, NULL, 'Good', 'Tunis Warehouse', 2000, 'up', 'kg', 0.6, 20, b'0', 7);

-- --------------------------------------------------------

--
-- Structure de la table `stock_items`
--

CREATE TABLE `stock_items` (
  `id` bigint(20) NOT NULL,
  `ai_insight` varchar(255) DEFAULT NULL,
  `category` varchar(255) NOT NULL,
  `condition_label` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `name` varchar(255) NOT NULL,
  `quantity` int(11) NOT NULL,
  `status` enum('listed','reserved','unlisted') NOT NULL,
  `unit` varchar(255) NOT NULL,
  `enterprise_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `stock_items`
--

INSERT INTO `stock_items` (`id`, `ai_insight`, `category`, `condition_label`, `created_at`, `name`, `quantity`, `status`, `unit`, `enterprise_id`) VALUES
(1, 'Shortage in 2 weeks', 'Metal', 'Good', '2026-04-09 17:40:46.000000', 'Aluminum Scrap', 2000, 'listed', 'kg', 1);

-- --------------------------------------------------------

--
-- Structure de la table `stock_movement`
--

CREATE TABLE `stock_movement` (
  `id` bigint(20) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `movement_date` datetime(6) DEFAULT NULL,
  `movement_type` varchar(255) DEFAULT NULL,
  `quantity` int(11) NOT NULL,
  `id_stock` bigint(20) DEFAULT NULL,
  `status` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `stock_movement`
--

INSERT INTO `stock_movement` (`id`, `description`, `movement_date`, `movement_type`, `quantity`, `id_stock`, `status`) VALUES
(1, '[TEST] Entree stock aluminium pour module annonces', '2026-04-26 19:50:26.000000', 'IN', 2500, 1, 'ACTIVE'),
(2, '[TEST] Sortie partielle aluminium apres reservation', '2026-04-24 19:50:26.000000', 'OUT', 300, 1, 'ACTIVE'),
(3, '[TEST] Entree stock PET pour module annonces', '2026-04-26 19:50:26.000000', 'IN', 1600, 2, 'ACTIVE');

-- --------------------------------------------------------

--
-- Structure de la table `transaction`
--

CREATE TABLE `transaction` (
  `idtransaction` bigint(20) NOT NULL,
  `amount` double DEFAULT NULL,
  `date` varchar(255) DEFAULT NULL,
  `enterprise_id` bigint(20) DEFAULT NULL,
  `project` varchar(255) DEFAULT NULL,
  `status` enum('PENDING','COMPLETED','FAILED','LOCKED') DEFAULT NULL,
  `type` enum('PAYMENT','DISBURSEMENT','REFUND','FEE','ESCROW','LOAN') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `transaction`
--

INSERT INTO `transaction` (`idtransaction`, `amount`, `date`, `enterprise_id`, `project`, `status`, `type`) VALUES
(1, 466.48, '2026-04-25', 4, 'ddh', 'PENDING', 'PAYMENT'),
(2, 466.48, '2026-04-25', 4, 'qqqq', 'PENDING', 'PAYMENT'),
(3, 116.62, '2026-04-25', 4, 'hggiu', 'PENDING', 'DISBURSEMENT'),
(4, 816.34, '2026-04-25', 4, 'abc', 'PENDING', 'PAYMENT'),
(5, 116.62, '2026-04-26', 4, 'qdqd', 'PENDING', 'PAYMENT'),
(6, 106.82, '2026-04-28', NULL, 'sisi', 'COMPLETED', 'DISBURSEMENT'),
(7, 1049.58, '2026-04-28', 4, 'izzz', 'PENDING', 'PAYMENT');

-- --------------------------------------------------------

--
-- Structure de la table `transporters`
--

CREATE TABLE `transporters` (
  `id` bigint(20) NOT NULL,
  `company_name` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `listings_count` int(11) NOT NULL,
  `orders_count` int(11) NOT NULL,
  `revenue` varchar(255) DEFAULT NULL,
  `sector` varchar(255) DEFAULT NULL,
  `tax_id` varchar(255) DEFAULT NULL,
  `user_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `transporters`
--

INSERT INTO `transporters` (`id`, `company_name`, `created_at`, `listings_count`, `orders_count`, `revenue`, `sector`, `tax_id`, `user_id`) VALUES
(1, 'Karim Logistics', '2026-04-09 15:23:41.000000', 0, 34, '8750', 'Transport & Logistics', 'TN456', 3);

-- --------------------------------------------------------

--
-- Structure de la table `transport_offers`
--

CREATE TABLE `transport_offers` (
  `id` bigint(20) NOT NULL,
  `cargo_description` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `from_location` varchar(255) NOT NULL,
  `proposed_earn` decimal(14,2) NOT NULL,
  `status` enum('open','assigned','closed') NOT NULL,
  `to_location` varchar(255) NOT NULL,
  `weight_label` varchar(255) NOT NULL,
  `transporter_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `transport_offers`
--

INSERT INTO `transport_offers` (`id`, `cargo_description`, `created_at`, `from_location`, `proposed_earn`, `status`, `to_location`, `weight_label`, `transporter_id`) VALUES
(1, 'Steel Offcuts 2T', '2026-04-09 15:23:41.000000', 'Gabès', 420.00, 'open', 'Tunis', '2,000kg', 1);

-- --------------------------------------------------------

--
-- Structure de la table `users`
--

CREATE TABLE `users` (
  `id` bigint(20) NOT NULL,
  `account_status` enum('active','pending','suspended') DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `full_name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `role` enum('ROLE_ADMIN','ROLE_ENTERPRISE','ROLE_TRANSPORTER') NOT NULL,
  `verified` bit(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `users`
--

INSERT INTO `users` (`id`, `account_status`, `city`, `created_at`, `email`, `enabled`, `full_name`, `password`, `phone`, `role`, `verified`) VALUES
(1, 'active', 'Tunis', '2026-04-09 15:20:15.000000', 'admin@marketplace.com', b'1', 'Admin Principal', '$2a$10$mXt7p3hwv4WQPwiDs7TWI.jx26XT1oOvJsx1ShONqqe056qelEYSG', '+21621392008', 'ROLE_ADMIN', b'1'),
(2, 'active', 'Tunis', '2026-04-09 15:20:15.000000', 'ranymmejri1@gmail.com', b'1', 'Ranym RHA', '$2a$10$nhK1JlFnSgPrNk7vUlZz1uF6hyQFSgdWr6XnVmyu/bT7g2tgnpsTC', '+21621392008', 'ROLE_ENTERPRISE', b'1'),
(3, 'active', 'Sfax', '2026-04-09 15:23:41.000000', 'karim@transport.tn', b'1', 'Karim Transport', '$2a$10$cMDk6Ko6XMIXW/UCn.1oAeP0LoRCCUDsFmYY60MUa7RTVeJ6ulKPG', '+216 72 345 678', 'ROLE_TRANSPORTER', b'1'),
(4, 'active', NULL, '2026-04-22 10:39:58.000000', 'jean@entreprise.com', b'1', 'Jean Dupont', '$2a$10$9N53b/9dwsb3vCPtKSEpUOLE5zS5luL9cT3aScNFk/Gp5OSRvmNQe', '+21600000000', 'ROLE_ENTERPRISE', b'0'),
(5, 'active', NULL, '2026-04-22 20:02:53.000000', 'nadamansour721@gmail.com', b'1', 'nada mansour TEXTILE', '$2a$10$1v572ahOp6zuobvj5merRuzCVhhEC4FDBwCTVor6da/XERbRrO8yu', '+21621392008', 'ROLE_ENTERPRISE', b'1'),
(6, 'active', 'Sousse', '2026-04-26 19:50:25.000000', 'sirinerhaiem03@gmail.com', b'1', 'sirine', '$2a$10$7QJ8aGVVJJbXgV1ObE9OeODMo3lx56iK9b.FHF2YqjeFiJ6x8tTfK', '+21695275001', 'ROLE_ENTERPRISE', b'1'),
(7, 'active', 'Sfax', '2026-04-26 19:50:25.000000', 'izzatamri2@gmail.com', b'1', 'izzaat', '$2a$10$7QJ8aGVVJJbXgV1ObE9OeODMo3lx56iK9b.FHF2YqjeFiJ6x8tTfK$2a$10$oLe8Dr5TcgLfvPVnLETsW.O6jBHxJY7KGl3D3173dbAuvUM8CiCU2', '6654789325', 'ROLE_ENTERPRISE', b'1'),
(8, 'active', 'Bizerte', '2026-04-26 19:50:25.000000', 'rayabendhifi@gmail.com\n', b'1', 'Raya Recyclage', '$2a$10$nhK1JlFnSgPrNk7vUlZz1uF6hyQFSgdWr6XnVmyu/bT7g2tgnpsTC', '+216 70 555 666', 'ROLE_ADMIN', b'1'),
(9, 'active', 'Tunis', '2026-04-27 19:58:19.000000', 'slim@entreprise.tn', b'1', 'Slim Ben Ali', '$2a$10$1v572ahOp6zuobvj5merRuzCVhhEC4FDBwCTVor6da/XERbRrO8yu', '+216 71 234 567', 'ROLE_ENTERPRISE', b'1');

-- --------------------------------------------------------

--
-- Structure de la table `wallet_transactions`
--

CREATE TABLE `wallet_transactions` (
  `id` bigint(20) NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `from_party` varchar(255) DEFAULT NULL,
  `label` varchar(255) NOT NULL,
  `positive_flag` bit(1) DEFAULT NULL,
  `status` enum('completed','locked','pending') NOT NULL,
  `to_party` varchar(255) DEFAULT NULL,
  `type_label` varchar(255) NOT NULL,
  `value_date` date DEFAULT NULL,
  `user_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `wallet_transactions`
--

INSERT INTO `wallet_transactions` (`id`, `amount`, `created_at`, `from_party`, `label`, `positive_flag`, `status`, `to_party`, `type_label`, `value_date`, `user_id`) VALUES
(1, -54.00, '2026-04-09 15:23:41.000000', NULL, 'Platform commission', b'0', 'completed', NULL, 'Fee', '2025-03-14', 2),
(2, -54.00, '2026-04-11 10:06:41.000000', NULL, 'Platform commission', b'0', 'completed', NULL, 'Fee', '2025-03-14', 2),
(3, 1200.00, '2026-04-27 19:58:19.000000', NULL, 'Aluminum Scrap sale', b'1', 'completed', NULL, 'Escrow Release', '2025-03-14', 9);

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `comments`
--
ALTER TABLE `comments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKlri30okf66phtcgbe5pok7cc0` (`parent_id`),
  ADD KEY `FK8omq0tc18jd43bu5tjh6jvraq` (`user_id`),
  ADD KEY `FK7vjjesvpaui5sytd4ycp62jr6` (`listing_id`);

--
-- Index pour la table `deliveries`
--
ALTER TABLE `deliveries`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK5vsv4wdyof55t0e3a26jy5xfu` (`enterprise_id`),
  ADD KEY `FKra6u1owihjkw7xh2c56v7uxir` (`transporter_id`);

--
-- Index pour la table `delivery_orders`
--
ALTER TABLE `delivery_orders`
  ADD PRIMARY KEY (`id_delivery`);

--
-- Index pour la table `enterprises`
--
ALTER TABLE `enterprises`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_8d4ninni1puu1j5jg1rromp9s` (`user_id`);

--
-- Index pour la table `escrow`
--
ALTER TABLE `escrow`
  ADD PRIMARY KEY (`idescrow`);

--
-- Index pour la table `exchange_requests`
--
ALTER TABLE `exchange_requests`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKdr5godwrt0m4fh9u28buoc6ll` (`recipient_enterprise_id`);

--
-- Index pour la table `favorites`
--
ALTER TABLE `favorites`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKi9yqqgg83yv72ftta12j4nfws` (`user_id`,`listing_id`),
  ADD KEY `FKjq6bbdk2g9lhv2b1uh532n5dp` (`listing_id`);

--
-- Index pour la table `financing_request`
--
ALTER TABLE `financing_request`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `group_participants`
--
ALTER TABLE `group_participants`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK7xwgqdfdvowafkdfo7f1yy80p` (`group_id`,`company_id`);

--
-- Index pour la table `group_purchases`
--
ALTER TABLE `group_purchases`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_12jeqttg6hmkegavngd5t2i0u` (`listing_id`);

--
-- Index pour la table `inventory_scan`
--
ALTER TABLE `inventory_scan`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKriv9n1i3wlm43h4jb4d1sl9ng` (`id_product`);

--
-- Index pour la table `invoice`
--
ALTER TABLE `invoice`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_t6xkdjx1qtd5whp2iljdfn2yj` (`invoice_number`);

--
-- Index pour la table `listings`
--
ALTER TABLE `listings`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKkfc9m997no0h5jv1bgd2p6gul` (`enterprise_id`);

--
-- Index pour la table `platform_events`
--
ALTER TABLE `platform_events`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `post_attachments`
--
ALTER TABLE `post_attachments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKfkxxqtdtvm86rb2c5l0ejie4a` (`listing_id`);

--
-- Index pour la table `product`
--
ALTER TABLE `product`
  ADD PRIMARY KEY (`id_product`),
  ADD KEY `FK47m71l5cmgm7ne44n04c4s3cy` (`enterprise_id`);

--
-- Index pour la table `reclamations`
--
ALTER TABLE `reclamations`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKrw7xibfc2yy8uoak8st5f045s` (`enterprise_id`),
  ADD KEY `FKsiddydhpj69ljev69919712xs` (`product_id`),
  ADD KEY `FK63cqd9t0k9a0fstp8s5o2dhgw` (`stock_item_id`),
  ADD KEY `FKl0w1xe82pex0186ywrfpjxomd` (`target_enterprise_id`);

--
-- Index pour la table `reservations`
--
ALTER TABLE `reservations`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKnc91a1pkbaidc0ei29b4ah3md` (`enterprise_id`);

--
-- Index pour la table `resource_listings`
--
ALTER TABLE `resource_listings`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK7ltjjhi6yowk9957xtk8cpvvh` (`product_id`);

--
-- Index pour la table `shipments`
--
ALTER TABLE `shipments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKlke35ceh1ob6uo4ph77ai0xy` (`delivery_order_id`);

--
-- Index pour la table `solidarity_associations`
--
ALTER TABLE `solidarity_associations`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `stock_item`
--
ALTER TABLE `stock_item`
  ADD PRIMARY KEY (`id_stock`),
  ADD KEY `FKftr55dgb2sl08ke2x0tpdy4ix` (`id_product`),
  ADD KEY `FK8u3uspnci2ub7tv60o2ylstmr` (`enterprise_id`);

--
-- Index pour la table `stock_items`
--
ALTER TABLE `stock_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKkoes77atih3h3lmq1xrx8jr4j` (`enterprise_id`);

--
-- Index pour la table `stock_movement`
--
ALTER TABLE `stock_movement`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK4riel2r90qx3uxpdw4joujqht` (`id_stock`);

--
-- Index pour la table `transaction`
--
ALTER TABLE `transaction`
  ADD PRIMARY KEY (`idtransaction`);

--
-- Index pour la table `transporters`
--
ALTER TABLE `transporters`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_eb1bqpvlqmmb0yynjin9nxpyu` (`user_id`);

--
-- Index pour la table `transport_offers`
--
ALTER TABLE `transport_offers`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKnhm564gunwbmxyc8rvwk8d77h` (`transporter_id`);

--
-- Index pour la table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_6dotkott2kjsp8vw4d0m25fb7` (`email`);

--
-- Index pour la table `wallet_transactions`
--
ALTER TABLE `wallet_transactions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKrtsa3qtjhd0rn4xb92na03vd` (`user_id`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `comments`
--
ALTER TABLE `comments`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=132;

--
-- AUTO_INCREMENT pour la table `deliveries`
--
ALTER TABLE `deliveries`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `delivery_orders`
--
ALTER TABLE `delivery_orders`
  MODIFY `id_delivery` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT pour la table `enterprises`
--
ALTER TABLE `enterprises`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT pour la table `escrow`
--
ALTER TABLE `escrow`
  MODIFY `idescrow` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=33;

--
-- AUTO_INCREMENT pour la table `exchange_requests`
--
ALTER TABLE `exchange_requests`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `favorites`
--
ALTER TABLE `favorites`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=141;

--
-- AUTO_INCREMENT pour la table `financing_request`
--
ALTER TABLE `financing_request`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `group_participants`
--
ALTER TABLE `group_participants`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=23;

--
-- AUTO_INCREMENT pour la table `group_purchases`
--
ALTER TABLE `group_purchases`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=18;

--
-- AUTO_INCREMENT pour la table `inventory_scan`
--
ALTER TABLE `inventory_scan`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `invoice`
--
ALTER TABLE `invoice`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=26;

--
-- AUTO_INCREMENT pour la table `listings`
--
ALTER TABLE `listings`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `platform_events`
--
ALTER TABLE `platform_events`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `post_attachments`
--
ALTER TABLE `post_attachments`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=63;

--
-- AUTO_INCREMENT pour la table `product`
--
ALTER TABLE `product`
  MODIFY `id_product` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

--
-- AUTO_INCREMENT pour la table `reclamations`
--
ALTER TABLE `reclamations`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `reservations`
--
ALTER TABLE `reservations`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `resource_listings`
--
ALTER TABLE `resource_listings`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=96;

--
-- AUTO_INCREMENT pour la table `shipments`
--
ALTER TABLE `shipments`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT pour la table `solidarity_associations`
--
ALTER TABLE `solidarity_associations`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `stock_item`
--
ALTER TABLE `stock_item`
  MODIFY `id_stock` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT pour la table `stock_items`
--
ALTER TABLE `stock_items`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `stock_movement`
--
ALTER TABLE `stock_movement`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT pour la table `transaction`
--
ALTER TABLE `transaction`
  MODIFY `idtransaction` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT pour la table `transporters`
--
ALTER TABLE `transporters`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `transport_offers`
--
ALTER TABLE `transport_offers`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT pour la table `wallet_transactions`
--
ALTER TABLE `wallet_transactions`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `comments`
--
ALTER TABLE `comments`
  ADD CONSTRAINT `FK7vjjesvpaui5sytd4ycp62jr6` FOREIGN KEY (`listing_id`) REFERENCES `resource_listings` (`id`),
  ADD CONSTRAINT `FKlri30okf66phtcgbe5pok7cc0` FOREIGN KEY (`parent_id`) REFERENCES `comments` (`id`),
  ADD CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `deliveries`
--
ALTER TABLE `deliveries`
  ADD CONSTRAINT `FK5vsv4wdyof55t0e3a26jy5xfu` FOREIGN KEY (`enterprise_id`) REFERENCES `enterprises` (`id`),
  ADD CONSTRAINT `FKra6u1owihjkw7xh2c56v7uxir` FOREIGN KEY (`transporter_id`) REFERENCES `transporters` (`id`);

--
-- Contraintes pour la table `enterprises`
--
ALTER TABLE `enterprises`
  ADD CONSTRAINT `FKtegoe6oujp8lc23w368lii1i4` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Contraintes pour la table `exchange_requests`
--
ALTER TABLE `exchange_requests`
  ADD CONSTRAINT `FKdr5godwrt0m4fh9u28buoc6ll` FOREIGN KEY (`recipient_enterprise_id`) REFERENCES `enterprises` (`id`);

--
-- Contraintes pour la table `favorites`
--
ALTER TABLE `favorites`
  ADD CONSTRAINT `FKjq6bbdk2g9lhv2b1uh532n5dp` FOREIGN KEY (`listing_id`) REFERENCES `resource_listings` (`id`),
  ADD CONSTRAINT `FKk7du8b8ewipawnnpg76d55fus` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Contraintes pour la table `group_participants`
--
ALTER TABLE `group_participants`
  ADD CONSTRAINT `FKlq3l59a6axronfgr6g9vrx0tv` FOREIGN KEY (`group_id`) REFERENCES `group_purchases` (`id`);

--
-- Contraintes pour la table `group_purchases`
--
ALTER TABLE `group_purchases`
  ADD CONSTRAINT `FKad0a54p6t24lvwrovo64phous` FOREIGN KEY (`listing_id`) REFERENCES `resource_listings` (`id`);

--
-- Contraintes pour la table `inventory_scan`
--
ALTER TABLE `inventory_scan`
  ADD CONSTRAINT `FKriv9n1i3wlm43h4jb4d1sl9ng` FOREIGN KEY (`id_product`) REFERENCES `product` (`id_product`);

--
-- Contraintes pour la table `listings`
--
ALTER TABLE `listings`
  ADD CONSTRAINT `FKkfc9m997no0h5jv1bgd2p6gul` FOREIGN KEY (`enterprise_id`) REFERENCES `enterprises` (`id`);

--
-- Contraintes pour la table `post_attachments`
--
ALTER TABLE `post_attachments`
  ADD CONSTRAINT `FKfkxxqtdtvm86rb2c5l0ejie4a` FOREIGN KEY (`listing_id`) REFERENCES `resource_listings` (`id`);

--
-- Contraintes pour la table `product`
--
ALTER TABLE `product`
  ADD CONSTRAINT `FK47m71l5cmgm7ne44n04c4s3cy` FOREIGN KEY (`enterprise_id`) REFERENCES `enterprises` (`id`);

--
-- Contraintes pour la table `reclamations`
--
ALTER TABLE `reclamations`
  ADD CONSTRAINT `FK63cqd9t0k9a0fstp8s5o2dhgw` FOREIGN KEY (`stock_item_id`) REFERENCES `stock_item` (`id_stock`),
  ADD CONSTRAINT `FKl0w1xe82pex0186ywrfpjxomd` FOREIGN KEY (`target_enterprise_id`) REFERENCES `enterprises` (`id`),
  ADD CONSTRAINT `FKrw7xibfc2yy8uoak8st5f045s` FOREIGN KEY (`enterprise_id`) REFERENCES `enterprises` (`id`),
  ADD CONSTRAINT `FKsiddydhpj69ljev69919712xs` FOREIGN KEY (`product_id`) REFERENCES `product` (`id_product`);

--
-- Contraintes pour la table `reservations`
--
ALTER TABLE `reservations`
  ADD CONSTRAINT `FKnc91a1pkbaidc0ei29b4ah3md` FOREIGN KEY (`enterprise_id`) REFERENCES `enterprises` (`id`);

--
-- Contraintes pour la table `resource_listings`
--
ALTER TABLE `resource_listings`
  ADD CONSTRAINT `FK7ltjjhi6yowk9957xtk8cpvvh` FOREIGN KEY (`product_id`) REFERENCES `product` (`id_product`);

--
-- Contraintes pour la table `shipments`
--
ALTER TABLE `shipments`
  ADD CONSTRAINT `FKlke35ceh1ob6uo4ph77ai0xy` FOREIGN KEY (`delivery_order_id`) REFERENCES `delivery_orders` (`id_delivery`);

--
-- Contraintes pour la table `stock_item`
--
ALTER TABLE `stock_item`
  ADD CONSTRAINT `FK8u3uspnci2ub7tv60o2ylstmr` FOREIGN KEY (`enterprise_id`) REFERENCES `enterprises` (`id`),
  ADD CONSTRAINT `FKftr55dgb2sl08ke2x0tpdy4ix` FOREIGN KEY (`id_product`) REFERENCES `product` (`id_product`);

--
-- Contraintes pour la table `stock_items`
--
ALTER TABLE `stock_items`
  ADD CONSTRAINT `FKkoes77atih3h3lmq1xrx8jr4j` FOREIGN KEY (`enterprise_id`) REFERENCES `enterprises` (`id`);

--
-- Contraintes pour la table `stock_movement`
--
ALTER TABLE `stock_movement`
  ADD CONSTRAINT `FK4riel2r90qx3uxpdw4joujqht` FOREIGN KEY (`id_stock`) REFERENCES `stock_item` (`id_stock`);

--
-- Contraintes pour la table `transporters`
--
ALTER TABLE `transporters`
  ADD CONSTRAINT `FK43hlixrg4tj79t7r99wqjygio` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Contraintes pour la table `transport_offers`
--
ALTER TABLE `transport_offers`
  ADD CONSTRAINT `FKnhm564gunwbmxyc8rvwk8d77h` FOREIGN KEY (`transporter_id`) REFERENCES `transporters` (`id`);

--
-- Contraintes pour la table `wallet_transactions`
--
ALTER TABLE `wallet_transactions`
  ADD CONSTRAINT `FKrtsa3qtjhd0rn4xb92na03vd` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
