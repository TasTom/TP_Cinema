ALTER TABLE films
    ADD COLUMN synopsis TEXT NULL,
    ADD COLUMN realisateur VARCHAR(255) NULL,
    ADD COLUMN affiche VARCHAR(255) NULL,
    ADD COLUMN panorama VARCHAR(255) NULL,
    ADD COLUMN source_externe VARCHAR(20) NULL,
    ADD COLUMN identifiant_externe VARCHAR(100) NULL,
    ADD COLUMN annee_sortie INT NULL,
    ADD CONSTRAINT uk_films_source UNIQUE (source_externe, identifiant_externe);
