package fr.butinfo.cinema;

import fr.butinfo.cinema.film.Film;
import fr.butinfo.cinema.film.FilmRepository;
import fr.butinfo.cinema.reservation.ReservationRepository;
import fr.butinfo.cinema.reservation.StatutReservation;
import fr.butinfo.cinema.salle.Salle;
import fr.butinfo.cinema.salle.SalleRepository;
import fr.butinfo.cinema.seance.Seance;
import fr.butinfo.cinema.seance.SeanceRepository;
import fr.butinfo.cinema.support.TestEntities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@SuppressWarnings("rawtypes")
class CinemaIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:9.7");

    @LocalServerPort
    private int port;

    @Autowired
    private ReservationRepository reservations;

    @Autowired
    private SeanceRepository seances;

    @Autowired
    private SalleRepository salles;

    @Autowired
    private FilmRepository films;

    private RestClient rest;

    @BeforeEach
    void preparer() {
        rest = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
        reservations.deleteAll();
        seances.deleteAll();
        salles.deleteAll();
        films.deleteAll();
    }

    @Test
    void exposeLaLecturePubliqueLaSanteEtLaDocumentation() {
        ResponseEntity<String> filmsResponse = getString("/api/films");
        ResponseEntity<String> seancesResponse = getString("/api/seances");
        ResponseEntity<String> healthResponse = getString("/actuator/health");
        ResponseEntity<String> apiDocsResponse = getString("/v3/api-docs");
        ResponseEntity<String> swaggerResponse = getString("/swagger-ui.html");

        assertThat(filmsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(seancesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody()).contains("UP");
        assertThat(apiDocsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(apiDocsResponse.getBody()).contains("openapi");
        assertThat(swaggerResponse.getStatusCode().value()).isIn(200, 302);
    }

    @Test
    void creeUneReservationValidePuisLibereLesPlacesALAnnulation() {
        Seance seance = creerSeance(20, "9.90");

        ResponseEntity<Map> creation = post("/api/reservations", "client", "client123",
                TestEntities.reservationPayload(seance.getId(), "lea@example.test", 3));

        assertThat(creation.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String code = (String) creation.getBody().get("code");
        assertThat(code).isNotBlank();
        assertThat(creation.getBody().get("statut")).isEqualTo("CONFIRMEE");
        assertThat(((Number) creation.getBody().get("nombrePlaces")).intValue()).isEqualTo(3);
        assertThat(((Number) creation.getBody().get("montantTotal")).doubleValue()).isEqualTo(29.70);

        assertThat(placesRestantes(seance.getId())).isEqualTo(17);

        ResponseEntity<Void> annulation = delete("/api/reservations/" + code, "client", "client123");

        assertThat(annulation.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(placesRestantes(seance.getId())).isEqualTo(20);
        assertThat(reservations.sumPlacesBySeanceIdAndStatut(seance.getId(), StatutReservation.CONFIRMEE))
                .isZero();
    }

    @Test
    void valideLesPlacesEtRefuseLaSurreservation() {
        Seance seance = creerSeance(20, "9.90");

        ResponseEntity<Map> zero = post("/api/reservations", "client", "client123",
                TestEntities.reservationPayload(seance.getId(), "lea@example.test", 0));
        ResponseEntity<Map> neuf = post("/api/reservations", "client", "client123",
                TestEntities.reservationPayload(seance.getId(), "lea@example.test", 9));
        assertThat(zero.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(zero.getBody().get("code")).isEqualTo("VALIDATION_ECHOUEE");
        assertThat(neuf.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(neuf.getBody().get("code")).isEqualTo("VALIDATION_ECHOUEE");

        assertThat(post("/api/reservations", "client", "client123",
                TestEntities.reservationPayload(seance.getId(), "lea@example.test", 8))
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(post("/api/reservations", "client", "client123",
                TestEntities.reservationPayload(seance.getId(), "lea@example.test", 8))
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map> refus = post("/api/reservations", "client", "client123",
                TestEntities.reservationPayload(seance.getId(), "lea@example.test", 5));
        assertThat(refus.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(refus.getBody().get("code")).isEqualTo("CAPACITE_INSUFFISANTE");
        assertThat((String) refus.getBody().get("message")).contains("4 places");

        assertThat(reservations.sumPlacesBySeanceIdAndStatut(seance.getId(), StatutReservation.CONFIRMEE))
                .isEqualTo(16);
    }

    @Test
    void appliqueLesAutorisationsDesRoles() {
        Seance seance = creerSeance(20, "9.90");

        ResponseEntity<Map> anonyme = post("/api/reservations", null, null,
                TestEntities.reservationPayload(seance.getId(), "lea@example.test", 2));
        assertThat(anonyme.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        String nomSalle = "Salle " + UUID.randomUUID();
        Map<String, Object> sallePayload = Map.of("nom", nomSalle, "capacite", 50);
        ResponseEntity<Map> salleParClient = post("/api/admin/salles", "client", "client123", sallePayload);
        assertThat(salleParClient.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Map> salleParAdmin = post("/api/admin/salles", "admin", "admin123", sallePayload);
        assertThat(salleParAdmin.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(salleParAdmin.getHeaders().getLocation()).isNotNull();

        Map<String, Object> filmPayload = Map.of(
                "titre", "Film " + UUID.randomUUID(), "genre", "Drame", "dureeMinutes", 120);
        ResponseEntity<Map> filmParClient = post("/api/programmation/films", "client", "client123", filmPayload);
        assertThat(filmParClient.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Map> filmParProgrammateur = post("/api/programmation/films",
                "programmateur", "prog123", filmPayload);
        assertThat(filmParProgrammateur.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map> reservationParAdmin = post("/api/reservations", "admin", "admin123",
                TestEntities.reservationPayload(seance.getId(), "admin@example.test", 2));
        assertThat(reservationParAdmin.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void filtreLesFilmsParTitreEtGenre() {
        Film alpha = films.save(new Film("Alpha", "Drame", 120, LocalDate.of(2026, 1, 1)));
        films.save(new Film("Beta", "Comédie", 100, LocalDate.of(2026, 2, 2)));

        ResponseEntity<List> parTitre = getList("/api/films?titre=alp");
        ResponseEntity<List> parGenre = getList("/api/films?genre=Drame");

        assertThat(parTitre.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(parTitre.getBody()).hasSize(1);
        assertThat(((Map) parTitre.getBody().getFirst()).get("titre")).isEqualTo("Alpha");
        assertThat(parGenre.getBody()).hasSize(1);
        assertThat(((Map) parGenre.getBody().getFirst()).get("titre")).isEqualTo(alpha.getTitre());
    }

    @Test
    void creeUneSeanceParLaProgrammation() {
        Seance existante = creerSeance(20, "9.90");
        Map<String, Object> payload = Map.of(
                "filmId", existante.getFilm().getId(),
                "salleId", existante.getSalle().getId(),
                "debut", "2027-01-15T20:30:00",
                "prixUnitaire", new BigDecimal("10.50"));

        ResponseEntity<Map> creation = post("/api/programmation/seances",
                "programmateur", "prog123", payload);

        assertThat(creation.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(creation.getHeaders().getLocation()).isNotNull();
        assertThat(((Number) creation.getBody().get("prixUnitaire")).doubleValue()).isEqualTo(10.50);
    }

    private Seance creerSeance(int capacite, String prix) {
        Film film = films.save(new Film("Film " + UUID.randomUUID(), "Drame", 120, LocalDate.now()));
        Salle salle = salles.save(new Salle("Salle " + UUID.randomUUID(), capacite));
        return seances.save(new Seance(film, salle, LocalDateTime.now().plusDays(2),
                new BigDecimal(prix)));
    }

    private int placesRestantes(Long seanceId) {
        ResponseEntity<Map> response = getMap("/api/seances/" + seanceId);
        return ((Number) response.getBody().get("placesRestantes")).intValue();
    }

    private ResponseEntity<String> getString(String path) {
        return call(String.class, () -> rest.get().uri(path).retrieve().toEntity(String.class));
    }

    private ResponseEntity<List> getList(String path) {
        return call(List.class, () -> rest.get().uri(path).retrieve().toEntity(List.class));
    }

    private ResponseEntity<Map> getMap(String path) {
        return call(Map.class, () -> rest.get().uri(path).retrieve().toEntity(Map.class));
    }

    private ResponseEntity<Map> post(String path, String username, String password, Object payload) {
        return call(Map.class, () -> rest.post().uri(path)
                .headers(headers -> {
                    if (username != null) {
                        headers.setBasicAuth(username, password);
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toEntity(Map.class));
    }

    private ResponseEntity<Void> delete(String path, String username, String password) {
        return call(Void.class, () -> rest.delete().uri(path)
                .headers(headers -> headers.setBasicAuth(username, password))
                .retrieve()
                .toBodilessEntity());
    }

    private <T> ResponseEntity<T> call(Class<T> type, java.util.function.Supplier<ResponseEntity<T>> appel) {
        try {
            return appel.get();
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .headers(ex.getResponseHeaders())
                    .body(type == null ? null : ex.getResponseBodyAs(type));
        }
    }
}
