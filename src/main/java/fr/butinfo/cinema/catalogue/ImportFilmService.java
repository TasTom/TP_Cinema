package fr.butinfo.cinema.catalogue;

import fr.butinfo.cinema.common.error.DonneeInvalideException;
import fr.butinfo.cinema.common.error.ConflitMetierException;
import fr.butinfo.cinema.film.CreerFilmCommand;
import fr.butinfo.cinema.film.Film;
import fr.butinfo.cinema.film.FilmRepository;
import fr.butinfo.cinema.film.FilmService;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ImportFilmService {
    private final CatalogueExterneService catalogue;
    private final AffichesService affiches;
    private final FilmRepository films;
    private final FilmService service;
    private final TransactionTemplate transaction;

    public ImportFilmService(CatalogueExterneService catalogue, AffichesService affiches,
                              FilmRepository films, FilmService service, PlatformTransactionManager transactions) {
        this.catalogue = catalogue;
        this.affiches = affiches;
        this.films = films;
        this.service = service;
        this.transaction = new TransactionTemplate(transactions);
    }

    public Long importer(String source, String id) {
        FilmExterne externe = catalogue.trouver(source, id);
        if (externe.dureeMinutes() < 40 || externe.dureeMinutes() > 300 || externe.titre().isBlank()
                || externe.titre().length() > 255) {
            throw new DonneeInvalideException("FILM_EXTERNE_INVALIDE", "Ce film ne respecte pas la durée de 40 à 300 minutes requise pour la programmation.");
        }
        // Les téléchargements ne doivent pas prolonger une transaction MySQL.
        String affiche = affiches.conserver(externe.afficheUrl());
        String panorama = affiches.conserver(externe.panoramaUrl());
        try {
            return transaction.execute(statut -> {
            Film film = films.findBySourceExterneAndIdentifiantExterne(externe.source(), externe.id())
                    .or(() -> films.findFirstByTitreIgnoreCase(externe.titre())).orElseGet(() ->
                            service.creer(new CreerFilmCommand(externe.titre(),
                                    externe.source().equals("GHIBLI") ? "Animation" : "Cinéma", externe.dureeMinutes(), externe.dateSortie())));
            film.enrichir(externe.synopsis(), externe.realisateur(), affiche, panorama, externe.source(), externe.id(), externe.annee());
            return films.saveAndFlush(film).getId();
            });
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitMetierException("IMPORT_CONCURRENT", "Ce film a été modifié pendant l'import. Réessayez : le film déjà enregistré sera réutilisé.");
        }
    }
}
