package fr.butinfo.cinema.catalogue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class AffichesServiceTest {
    @TempDir Path repertoire;

    @Test
    void conserveUneImageValideEtEviteLeTelechargementAuDeuxiemeImport() {
        var client = org.springframework.web.client.RestClient.builder();
        var serveur = org.springframework.test.web.client.MockRestServiceServer.bindTo(client).build();
        byte[] png = new byte[]{(byte)137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 0};
        serveur.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything())
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(png, org.springframework.http.MediaType.IMAGE_PNG));
        var service = new AffichesService(repertoire.toString(), client.build());
        String chemin = service.conserver("https://image.tmdb.org/t/p/w500/test.png");
        assertThat(chemin).startsWith("/affiches/").endsWith(".png");
        assertThat(service.trouver(chemin.substring("/affiches/".length()))).hasBinaryContent(png);
        assertThat(service.conserver("https://image.tmdb.org/t/p/w500/test.png")).isEqualTo(chemin);
        serveur.verify();
    }

    @Test
    void refuseUnContenuHtmlMasqueEnImageEtUneImageTropVolumineuse() {
        var client = org.springframework.web.client.RestClient.builder();
        var serveur = org.springframework.test.web.client.MockRestServiceServer.bindTo(client).build();
        serveur.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything())
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess("<script>alert(1)</script>", org.springframework.http.MediaType.IMAGE_PNG));
        serveur.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything())
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(new byte[5_000_001], org.springframework.http.MediaType.IMAGE_PNG));
        var service = new AffichesService(repertoire.toString(), client.build());
        assertThat(service.conserver("https://image.tmdb.org/t/p/w500/test.png")).isNull();
        assertThat(service.conserver("https://image.tmdb.org/t/p/w500/test2.png")).isNull();
        serveur.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://image.tmdb.org/t/p/w500/a.jpg", "https://localhost/t/p/w500/a.jpg",
            "https://image.tmdb.org.evil.test/t/p/w500/a.jpg", "https://image.tmdb.org:444/t/p/w500/a.jpg",
            "https://user@image.tmdb.org/t/p/w500/a.jpg", "https://image.tmdb.org/t/p/w500/a.svg",
            "https://image.tmdb.org/t/p/w500/a.jpg?redirect=localhost", "file:///etc/passwd"})
    void refuseLesAdressesSortantDeLaListeAutoriseeSansReseau(String adresse) {
        var affiches = new AffichesService(repertoire.toString());
        assertThat(AffichesService.urlAutorisee(adresse)).isFalse();
        assertThat(affiches.conserver(adresse)).isNull();
    }

    @Test
    void accepteLeCdnEtRefuseLaTraverseeDeRepertoire() {
        assertThat(AffichesService.urlAutorisee("https://image.tmdb.org/t/p/w500/a-b_1.jpg")).isTrue();
        var affiches = new AffichesService(repertoire.toString());
        assertThat(affiches.trouver("../secret.txt")).isNull();
        assertThat(affiches.trouver("inexistant.jpg")).isNull();
    }
}
