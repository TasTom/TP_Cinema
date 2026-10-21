package fr.butinfo.cinema.catalogue;

import fr.butinfo.cinema.common.error.ConflitMetierException;
import fr.butinfo.cinema.film.Film;
import fr.butinfo.cinema.film.FilmRepository;
import fr.butinfo.cinema.film.FilmService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.*;

class ImportFilmServiceTest {
    @Test
    void traduitUnConflitDImportEtAnnuleLaTransactionSansExposerLeSql() {
        var catalogue = mock(CatalogueExterneService.class);
        var affiches = mock(AffichesService.class);
        var films = mock(FilmRepository.class);
        var service = mock(FilmService.class);
        var transactions = mock(PlatformTransactionManager.class);
        var statut = new SimpleTransactionStatus();
        when(transactions.getTransaction(any())).thenReturn(statut);
        when(catalogue.trouver("TMDB", "42")).thenReturn(new FilmExterne("42", "Film", "", "", 120, null, 2020, "", "", "TMDB"));
        when(service.creer(any())).thenReturn(new Film("Film", "Cinéma", 120, null));
        when(films.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Détail SQL confidentiel"));
        var imports = new ImportFilmService(catalogue, affiches, films, service, transactions);
        assertThatThrownBy(() -> imports.importer("TMDB", "42"))
                .isInstanceOfSatisfying(ConflitMetierException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("IMPORT_CONCURRENT");
                    assertThat(e.getMessage()).doesNotContain("SQL", "confidentiel");
                });
        verify(transactions).rollback(statut);
        verify(transactions, never()).commit(any());
    }
}
