-- ============================================================
-- SYSTÈME DE GESTION DE RÉSERVATION DE SALLE DE CONFÉRENCE
-- Script de création de la base de données MySQL
-- ============================================================

CREATE DATABASE IF NOT EXISTS conference_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE conference_db;

-- ============================================================
-- TABLE DES UTILISATEURS
-- ============================================================
CREATE TABLE IF NOT EXISTS utilisateurs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,
    telephone VARCHAR(20),
    role ENUM('ADMIN', 'RESPONSABLE', 'UTILISATEUR') DEFAULT 'UTILISATEUR',
    actif BOOLEAN DEFAULT TRUE,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    derniere_connexion TIMESTAMP NULL
);

-- ============================================================
-- TABLE DES SALLES
-- ============================================================
CREATE TABLE IF NOT EXISTS salles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(150) NOT NULL,
    description TEXT,
    capacite INT NOT NULL,
    localisation VARCHAR(200),
    etage VARCHAR(50),
    superficie DECIMAL(10,2),
    disposition_defaut ENUM('THEATRE','CLASSE','CONFERENCE','COCKTAIL','CABARET') DEFAULT 'CONFERENCE',
    actif BOOLEAN DEFAULT TRUE,
    image_path VARCHAR(500),
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE DES ÉQUIPEMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS equipements (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(150) NOT NULL,
    description TEXT,
    type ENUM('AUDIO','VIDEO','INFORMATIQUE','MOBILIER','AUTRE') NOT NULL,
    quantite_totale INT DEFAULT 1,
    quantite_disponible INT DEFAULT 1,
    etat ENUM('BON','DEFECTUEUX','EN_REPARATION') DEFAULT 'BON',
    numero_serie VARCHAR(100),
    date_acquisition DATE,
    actif BOOLEAN DEFAULT TRUE
);

-- ============================================================
-- TABLE DE LIAISON SALLE-ÉQUIPEMENTS (équipements fixes par salle)
-- ============================================================
CREATE TABLE IF NOT EXISTS salle_equipements (
    salle_id INT NOT NULL,
    equipement_id INT NOT NULL,
    quantite INT DEFAULT 1,
    PRIMARY KEY (salle_id, equipement_id),
    FOREIGN KEY (salle_id) REFERENCES salles(id) ON DELETE CASCADE,
    FOREIGN KEY (equipement_id) REFERENCES equipements(id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE DES RÉSERVATIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS reservations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL,
    salle_id INT NOT NULL,
    titre VARCHAR(200) NOT NULL,
    description TEXT,
    date_debut DATETIME NOT NULL,
    date_fin DATETIME NOT NULL,
    nombre_participants INT DEFAULT 1,
    disposition ENUM('THEATRE','CLASSE','CONFERENCE','COCKTAIL','CABARET') DEFAULT 'CONFERENCE',
    statut ENUM('EN_ATTENTE','CONFIRME','ANNULE','TERMINE') DEFAULT 'EN_ATTENTE',
    commentaires TEXT,
    code_reservation VARCHAR(20) UNIQUE,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id),
    FOREIGN KEY (salle_id) REFERENCES salles(id)
);

-- ============================================================
-- TABLE DE LIAISON RÉSERVATION-ÉQUIPEMENTS (équipements demandés)
-- ============================================================
CREATE TABLE IF NOT EXISTS reservation_equipements (
    reservation_id INT NOT NULL,
    equipement_id INT NOT NULL,
    quantite INT DEFAULT 1,
    PRIMARY KEY (reservation_id, equipement_id),
    FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
    FOREIGN KEY (equipement_id) REFERENCES equipements(id)
);

-- ============================================================
-- TABLE DES NOTIFICATIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL,
    reservation_id INT,
    type ENUM('CONFIRMATION','RAPPEL','ANNULATION','ALERTE','FIN_SESSION') NOT NULL,
    titre VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    lu BOOLEAN DEFAULT FALSE,
    date_envoi TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_lecture TIMESTAMP NULL,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id),
    FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE SET NULL
);

-- ============================================================
-- TABLE DES SIGNALEMENTS D'ÉQUIPEMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS signalements (
    id INT AUTO_INCREMENT PRIMARY KEY,
    equipement_id INT NOT NULL,
    utilisateur_id INT NOT NULL,
    reservation_id INT,
    description TEXT NOT NULL,
    statut ENUM('OUVERT','EN_COURS','RESOLU') DEFAULT 'OUVERT',
    date_signalement TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_resolution TIMESTAMP NULL,
    FOREIGN KEY (equipement_id) REFERENCES equipements(id),
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id),
    FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE SET NULL
);

-- ============================================================
-- TABLE DES LOGS D'ACTIVITÉ
-- ============================================================
CREATE TABLE IF NOT EXISTS logs_activite (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    adresse_ip VARCHAR(45),
    date_action TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id) ON DELETE SET NULL
);

-- ============================================================
-- DONNÉES INITIALES
-- ============================================================

-- Utilisateur administrateur (mot de passe: Admin123!)
INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, telephone, role) VALUES
('Administrateur', 'Système', 'admin@conference.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lihC', '+216 70 000 000', 'ADMIN'),
('Dupont', 'Jean', 'jean.dupont@conference.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lihC', '+216 71 234 567', 'RESPONSABLE'),
('Martin', 'Sophie', 'sophie.martin@conference.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lihC', '+216 72 345 678', 'UTILISATEUR'),
('Ben Ali', 'Mohamed', 'med.benali@conference.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lihC', '+216 73 456 789', 'UTILISATEUR');

-- Salles de conférence
INSERT INTO salles (nom, description, capacite, localisation, etage, superficie, disposition_defaut) VALUES
('Salle Méditerranée', 'Grande salle de conférence principale avec vue panoramique', 100, 'Bâtiment A', '3ème étage', 150.00, 'THEATRE'),
('Salle Innovation', 'Salle moderne équipée pour les présentations high-tech', 30, 'Bâtiment B', '1er étage', 60.00, 'CONFERENCE'),
('Salle Carthage', 'Salle polyvalente idéale pour les formations', 50, 'Bâtiment A', '2ème étage', 80.00, 'CLASSE'),
('Salle Executive', 'Salle de réunion pour les réunions de direction', 15, 'Bâtiment C', 'RDC', 35.00, 'CONFERENCE'),
('Amphithéâtre Central', 'Grand amphithéâtre pour séminaires et conférences', 200, 'Bâtiment Principal', 'RDC', 300.00, 'THEATRE'),
('Salle Formation', 'Salle dédiée aux formations avec postes informatiques', 20, 'Bâtiment B', '2ème étage', 45.00, 'CLASSE');

-- Équipements
INSERT INTO equipements (nom, description, type, quantite_totale, quantite_disponible, etat) VALUES
('Projecteur HD 4K', 'Projecteur haute définition 4K avec télécommande', 'VIDEO', 5, 5, 'BON'),
('Écran de projection 3m', 'Écran motorisé 3 mètres', 'VIDEO', 4, 4, 'BON'),
('Microphone sans fil', 'Microphone HF avec récepteur', 'AUDIO', 8, 7, 'BON'),
('Système de sonorisation', 'Enceintes amplifiées 200W', 'AUDIO', 3, 3, 'BON'),
('Tableau blanc interactif', 'Tableau interactif SMART Board 75"', 'VIDEO', 3, 2, 'BON'),
('Ordinateur portable', 'Laptop Dell i7 16Go RAM', 'INFORMATIQUE', 6, 6, 'BON'),
('Webcam HD', 'Caméra Full HD pour visioconférence', 'INFORMATIQUE', 4, 4, 'BON'),
('Hub USB-C', 'Hub de connexion multiports', 'INFORMATIQUE', 10, 10, 'BON'),
('Tableau blanc magnétique', 'Tableau blanc avec marqueurs effaçables', 'MOBILIER', 6, 6, 'BON'),
('Paperboard', 'Chevalet paperboard avec bloc de papier', 'MOBILIER', 5, 5, 'BON'),
('Microphone de conférence', 'Micro de table omnidirectionnel pour conférence', 'AUDIO', 4, 4, 'BON'),
('Laser pointer', 'Pointeur laser vert avec télécommande slide', 'INFORMATIQUE', 8, 8, 'BON');

-- Équipements par salle
INSERT INTO salle_equipements (salle_id, equipement_id, quantite) VALUES
(1, 1, 2), (1, 2, 2), (1, 3, 4), (1, 4, 1), (1, 9, 2), (1, 10, 2),
(2, 1, 1), (2, 5, 1), (2, 6, 2), (2, 7, 1), (2, 11, 1),
(3, 1, 1), (3, 2, 1), (3, 3, 2), (3, 9, 2), (3, 10, 2),
(4, 5, 1), (4, 11, 1), (4, 9, 1),
(5, 1, 3), (5, 2, 2), (5, 3, 6), (5, 4, 2),
(6, 6, 20), (6, 7, 2), (6, 8, 20), (6, 9, 2);

-- Quelques réservations de démonstration
INSERT INTO reservations (utilisateur_id, salle_id, titre, description, date_debut, date_fin, nombre_participants, disposition, statut, code_reservation) VALUES
(2, 1, 'Réunion Stratégique Q1', 'Réunion de planification stratégique pour le premier trimestre', '2026-03-10 09:00:00', '2026-03-10 12:00:00', 45, 'CONFERENCE', 'CONFIRME', 'RES-2026-001'),
(3, 2, 'Formation JavaFX', 'Formation technique pour l\'équipe développement', '2026-03-11 14:00:00', '2026-03-11 17:00:00', 25, 'CLASSE', 'CONFIRME', 'RES-2026-002'),
(4, 4, 'Réunion Direction', 'Réunion mensuelle du comité de direction', '2026-03-12 10:00:00', '2026-03-12 11:30:00', 10, 'CONFERENCE', 'EN_ATTENTE', 'RES-2026-003'),
(2, 3, 'Séminaire Marketing', 'Séminaire annuel du département marketing', '2026-03-15 08:30:00', '2026-03-15 17:30:00', 40, 'THEATRE', 'CONFIRME', 'RES-2026-004'),
(3, 6, 'Formation Sécurité', 'Formation obligatoire sécurité informatique', '2026-03-18 09:00:00', '2026-03-18 16:00:00', 18, 'CLASSE', 'CONFIRME', 'RES-2026-005');

-- Notifications initiales
INSERT INTO notifications (utilisateur_id, reservation_id, type, titre, message) VALUES
(2, 1, 'CONFIRMATION', 'Réservation Confirmée', 'Votre réservation "Réunion Stratégique Q1" pour le 10/03/2026 a été confirmée.'),
(3, 2, 'CONFIRMATION', 'Réservation Confirmée', 'Votre réservation "Formation JavaFX" pour le 11/03/2026 a été confirmée.'),
(4, 3, 'RAPPEL', 'Rappel de Réservation', 'Rappel: Votre réunion "Réunion Direction" est prévue demain à 10h00.');

-- ============================================================
-- PROCÉDURES STOCKÉES
-- ============================================================

DELIMITER //

-- Procédure pour générer un code de réservation
CREATE PROCEDURE IF NOT EXISTS generer_code_reservation(OUT code VARCHAR(20))
BEGIN
    SET code = CONCAT('RES-', YEAR(NOW()), '-', LPAD(FLOOR(RAND() * 99999), 5, '0'));
END //

-- Procédure pour vérifier la disponibilité d'une salle
CREATE PROCEDURE IF NOT EXISTS verifier_disponibilite(
    IN p_salle_id INT,
    IN p_date_debut DATETIME,
    IN p_date_fin DATETIME,
    IN p_reservation_id INT,
    OUT p_disponible BOOLEAN
)
BEGIN
    DECLARE count_conflits INT;
    
    SELECT COUNT(*) INTO count_conflits
    FROM reservations
    WHERE salle_id = p_salle_id
    AND statut NOT IN ('ANNULE')
    AND id != COALESCE(p_reservation_id, -1)
    AND NOT (date_fin <= p_date_debut OR date_debut >= p_date_fin);
    
    SET p_disponible = (count_conflits = 0);
END //

DELIMITER ;

-- ============================================================
-- VUES UTILES
-- ============================================================

CREATE OR REPLACE VIEW vue_reservations_details AS
SELECT 
    r.id,
    r.code_reservation,
    r.titre,
    r.description,
    r.date_debut,
    r.date_fin,
    r.nombre_participants,
    r.disposition,
    r.statut,
    r.date_creation,
    s.nom AS salle_nom,
    s.capacite AS salle_capacite,
    s.localisation AS salle_localisation,
    u.nom AS utilisateur_nom,
    u.prenom AS utilisateur_prenom,
    u.email AS utilisateur_email,
    TIMESTAMPDIFF(MINUTE, r.date_debut, r.date_fin) AS duree_minutes
FROM reservations r
JOIN salles s ON r.salle_id = s.id
JOIN utilisateurs u ON r.utilisateur_id = u.id;

CREATE OR REPLACE VIEW vue_statistiques_salles AS
SELECT 
    s.id,
    s.nom,
    COUNT(r.id) AS total_reservations,
    AVG(TIMESTAMPDIFF(MINUTE, r.date_debut, r.date_fin)) AS duree_moyenne_minutes,
    SUM(TIMESTAMPDIFF(MINUTE, r.date_debut, r.date_fin)) AS total_minutes_utilisees
FROM salles s
LEFT JOIN reservations r ON s.id = r.salle_id AND r.statut = 'CONFIRME'
GROUP BY s.id, s.nom;
