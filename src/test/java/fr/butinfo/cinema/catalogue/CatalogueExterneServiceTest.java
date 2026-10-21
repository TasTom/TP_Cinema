package fr.butinfo.cinema.catalogue;

import fr.butinfo.cinema.common.error.ConflitMetierException;
import fr.butinfo.cinema.common.error.DonneeInvalideException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CatalogueExterneServiceTest {
    private final RestClient.Builder client = RestClient.builder();
    private final MockRestServiceServer serveur = MockRestServiceServer.bindTo(client).build();
    private final CatalogueExterneService catalogue = new CatalogueExterneService(JsonMapper.builder().build(), "jeton-test", client.build());

    @Test
    void traduitLesTitresGhibliFiltreEtReutiliseLeCache() {
        serveur.expect(requestTo("https://ghibliapi.vercel.app/films")).andRespond(withSuccess("""
                [{"id":"11111111-1111-1111-1111-111111111111","title":"Spirited Away","running_time":"125",
                "release_date":"2001","image":"http://localhost/private","movie_banner":"https://image.tmdb.org/t/p/original/abc.jpg"}]
                """, MediaType.APPLICATION_JSON));
        FilmExterne film = catalogue.rechercher("GHIBLI", "Chihiro").getFirst();
        assertThat(film.titre()).isEqualTo("Le Voyage de Chihiro");
        assertThat(film.dureeMinutes()).isEqualTo(125);
        assertThat(film.dateSortie()).isNull();
        assertThat(film.annee()).isEqualTo(2001);
        assertThat(film.afficheUrl()).isEmpty();
        assertThat(catalogue.rechercher("GHIBLI", "inexistant")).isEmpty();
        serveur.verify();
    }

    @Test
    void recupereLeDetailTmdbEnFrancaisAvecJetonServeur() {
        serveur.expect(requestTo("https://api.themoviedb.org/3/movie/42?language=fr-FR"))
                .andExpect(header("Authorization", "Bearer jeton-test"))
                .andRespond(withSuccess("""
                {"id":42,"title":"Un film","runtime":120,"release_date":"2020-10-15","poster_path":"/poster.jpg","backdrop_path":"//localhost/secret.jpg","overview":"Synopsis"}
                """, MediaType.APPLICATION_JSON));
        FilmExterne film = catalogue.trouver("TMDB", "42");
        assertThat(film.dureeMinutes()).isEqualTo(120);
        assertThat(film.afficheUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg");
        assertThat(film.panoramaUrl()).isEmpty();
        assertThat(film.dateSortie()).hasToString("2020-10-15");
        serveur.verify();
    }

    @Test
    void rechercheTmdbSansTransmettreLeJetonDansLUrl() {
        serveur.expect(requestTo(org.hamcrest.Matchers.containsString("/3/search/movie?language=fr-FR&include_adult=false&page=1&query=Inception")))
                .andExpect(header("Authorization", "Bearer jeton-test"))
                .andRespond(withSuccess("{\"results\":[{\"id\":42,\"title\":\"Inception\"}]}", MediaType.APPLICATION_JSON));
        assertThat(catalogue.rechercher("TMDB", "Inception")).hasSize(1);
        serveur.verify();
    }

    @Test
    void transformeUneReponseMalformeeEnErreurMetierSansDetailTechnique() {
        serveur.expect(anything()).andRespond(withSuccess("pas du JSON", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> catalogue.rechercher("GHIBLI", ""))
                .isInstanceOfSatisfying(ConflitMetierException.class, e -> assertThat(e.getCode()).isEqualTo("CATALOGUE_INDISPONIBLE"));
    }

    @Test
    void borneLaTailleDesReponsesExternesAvantDecodage() {
        serveur.expect(anything()).andRespond(withSuccess(" ".repeat(2_000_001), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> catalogue.rechercher("GHIBLI", ""))
                .isInstanceOf(ConflitMetierException.class);
        serveur.verify();
    }

    @Test
    void refuseLesSourcesIdentifiantsEtRecherchesNonAutorisesSansReseau() {
        assertThatThrownBy(() -> catalogue.rechercher("http://localhost", "")).isInstanceOf(DonneeInvalideException.class);
        assertThatThrownBy(() -> catalogue.trouver("TMDB", "../42")).isInstanceOf(DonneeInvalideException.class);
        assertThatThrownBy(() -> catalogue.rechercher("GHIBLI", "x".repeat(81))).isInstanceOf(DonneeInvalideException.class);
        var sansJeton = new CatalogueExterneService(JsonMapper.builder().build(), "", client.build());
        assertThatThrownBy(() -> sansJeton.rechercher("TMDB", ""))
                .isInstanceOfSatisfying(DonneeInvalideException.class, e -> assertThat(e.getCode()).isEqualTo("TMDB_NON_CONFIGURE"));
        serveur.verify();
    }
}
