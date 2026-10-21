INSERT INTO films (id, titre, genre, duree_minutes, date_sortie)
VALUES (1, 'Le Voyage de Chihiro', 'Animation', 125, '2001-04-04'),
       (2, 'Inception', 'Science-fiction', 148, '2010-07-16'),
       (3, 'Les Choristes', 'Drame', 97, '2004-03-17');

INSERT INTO salles (id, nom, capacite)
VALUES (1, 'Salle Lumiere', 120),
       (2, 'Salle Melies', 80);

INSERT INTO seances (id, film_id, salle_id, debut, prix_unitaire)
VALUES (1, 1, 1, '2026-11-05 20:00:00.000000', 9.90),
       (2, 2, 1, '2026-11-06 20:30:00.000000', 11.50),
       (3, 3, 2, '2026-11-07 18:00:00.000000', 8.50);
