package fr.butinfo.cinema.catalogue;

import fr.butinfo.cinema.film.Film;
import fr.butinfo.cinema.film.FilmRepository;
import fr.butinfo.cinema.seance.SeanceRepository;
import fr.butinfo.cinema.common.error.DonneeInvalideException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ImportFilmIntegrationTest {
    @Container @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:9.7");
    @Autowired ImportFilmService imports;
    @Autowired FilmRepository films;
    @Autowired SeanceRepository seances;
    @MockitoBean CatalogueExterneService catalogue;
    @MockitoBean AffichesService affiches;

    @Test
    void enrichitUnFilmExistantSansDoublonNiSeanceInventee() {
        Film existant = films.saveAndFlush(new Film("Film importé", "Animation", 120, null));
        long nombreFilms = films.count();
        long nombreSeances = seances.count();
        when(catalogue.trouver("GHIBLI", "42")).thenReturn(new FilmExterne("42", "Film importé", "Synopsis", "Réalisateur", 120, null, 2001, "image", "fond", "GHIBLI"));
        when(affiches.conserver("image")).thenReturn("/affiches/test.jpg");
        Long premier = imports.importer("GHIBLI", "42");
        Long second = imports.importer("GHIBLI", "42");
        assertThat(premier).isEqualTo(existant.getId()).isEqualTo(second);
        assertThat(films.count()).isEqualTo(nombreFilms);
        assertThat(seances.count()).isEqualTo(nombreSeances);
        Film resultat = films.findById(premier).orElseThrow();
        assertThat(resultat.getSynopsis()).isEqualTo("Synopsis");
        assertThat(resultat.getAffiche()).isEqualTo("/affiches/test.jpg");
        assertThat(resultat.getDateSortie()).isNull();
        assertThat(resultat.getAnneeSortie()).isEqualTo(2001);
    }

    @Test
    void refuseUneDureeExterneHorsBornesAvantTelechargementEtInsertion() {
        when(catalogue.trouver("TMDB", "43")).thenReturn(new FilmExterne("43", "Court métrage", "", "", 20, null, 2020, "", "", "TMDB"));
        long avant = films.count();
        assertThatThrownBy(() -> imports.importer("TMDB", "43")).isInstanceOf(DonneeInvalideException.class);
        assertThat(films.count()).isEqualTo(avant);
        verifyNoInteractions(affiches);
    }
}
