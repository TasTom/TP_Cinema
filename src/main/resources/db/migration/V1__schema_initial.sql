CREATE TABLE films (
    id BIGINT NOT NULL AUTO_INCREMENT,
    titre VARCHAR(255) NOT NULL,
    genre VARCHAR(80) NULL,
    duree_minutes INT NOT NULL,
    date_sortie DATE NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_films_duree CHECK (duree_minutes BETWEEN 40 AND 300)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_films_titre ON films (titre);
CREATE INDEX idx_films_genre ON films (genre);

CREATE TABLE salles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nom VARCHAR(100) NOT NULL,
    capacite INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_salles_nom UNIQUE (nom),
    CONSTRAINT ck_salles_capacite CHECK (capacite BETWEEN 20 AND 500)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE seances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    film_id BIGINT NOT NULL,
    salle_id BIGINT NOT NULL,
    debut DATETIME(6) NOT NULL,
    prix_unitaire DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_seances_film FOREIGN KEY (film_id) REFERENCES films (id),
    CONSTRAINT fk_seances_salle FOREIGN KEY (salle_id) REFERENCES salles (id),
    CONSTRAINT ck_seances_prix CHECK (prix_unitaire > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_seances_debut ON seances (debut);
CREATE INDEX idx_seances_film ON seances (film_id);
CREATE INDEX idx_seances_salle ON seances (salle_id);

CREATE TABLE reservations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL,
    seance_id BIGINT NOT NULL,
    email_client VARCHAR(255) NOT NULL,
    nombre_places INT NOT NULL,
    statut VARCHAR(20) NOT NULL,
    montant_total DECIMAL(10, 2) NOT NULL,
    creee_le DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_reservations_code UNIQUE (code),
    CONSTRAINT fk_reservations_seance FOREIGN KEY (seance_id) REFERENCES seances (id),
    CONSTRAINT ck_reservations_places CHECK (nombre_places BETWEEN 1 AND 8),
    CONSTRAINT ck_reservations_statut CHECK (statut IN ('CONFIRMEE', 'ANNULEE')),
    CONSTRAINT ck_reservations_montant CHECK (montant_total >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_reservations_seance_statut ON reservations (seance_id, statut);
CREATE INDEX idx_reservations_email ON reservations (email_client);
