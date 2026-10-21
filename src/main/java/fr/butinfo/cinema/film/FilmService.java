package fr.butinfo.cinema.film;

import fr.butinfo.cinema.common.error.DonneeInvalideException;
import fr.butinfo.cinema.common.error.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FilmService {

    private static final int DUREE_MIN = 40;
    private static final int DUREE_MAX = 300;
    private static final int TITRE_MAX = 255;
    private static final int GENRE_MAX = 80;

    private final FilmRepository films;

    public FilmService(FilmRepository films) {
        this.films = films;
    }

    public List<Film> rechercher(String titre, String genre) {
        return films.rechercher(normaliser(titre), normaliser(genre));
    }

    public Film trouver(Long id) {
        if (id == null) {
            throw new RessourceIntrouvableException("FILM_INTROUVABLE", "Le film demandé n'existe pas.");
        }
        return films.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("FILM_INTROUVABLE",
                        "Le film " + id + " n'existe pas."));
    }

    @Transactional
    public Film creer(CreerFilmCommand command) {
        String titre = normaliser(command.titre());
        if (titre == null) {
            throw new DonneeInvalideException("VALIDATION_ECHOUEE", "Le titre du film est obligatoire.");
        }
        if (titre.length() > TITRE_MAX) {
            throw new DonneeInvalideException("VALIDATION_ECHOUEE",
                    "Le titre du film ne doit pas dépasser " + TITRE_MAX + " caractères.");
        }
        if (command.dureeMinutes() < DUREE_MIN || command.dureeMinutes() > DUREE_MAX) {
            throw new DonneeInvalideException("VALIDATION_ECHOUEE",
                    "La durée du film doit être comprise entre " + DUREE_MIN + " et " + DUREE_MAX + " minutes.");
        }
        String genre = normaliser(command.genre());
        if (genre != null && genre.length() > GENRE_MAX) {
            throw new DonneeInvalideException("VALIDATION_ECHOUEE",
                    "Le genre ne doit pas dépasser " + GENRE_MAX + " caractères.");
        }
        return films.save(new Film(titre, genre, command.dureeMinutes(), command.dateSortie()));
    }

    private static String normaliser(String valeur) {
        if (valeur == null) {
            return null;
        }
        String resultat = valeur.trim();
        return resultat.isEmpty() ? null : resultat;
    }
}
