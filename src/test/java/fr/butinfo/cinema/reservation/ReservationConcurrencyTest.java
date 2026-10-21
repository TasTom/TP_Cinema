package fr.butinfo.cinema.reservation;

import fr.butinfo.cinema.film.Film;
import fr.butinfo.cinema.film.FilmRepository;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@SuppressWarnings({"rawtypes", "unchecked"})
class ReservationConcurrencyTest {

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
    }

    @Test
    void deuxReservationsSimultaneesNeDepassentPasLaCapacite() throws Exception {
        reservations.deleteAll();
        seances.deleteAll();
        salles.deleteAll();
        films.deleteAll();

        Film film = films.save(new Film("Concurrence", "Test", 120, LocalDate.now()));
        Salle salle = salles.save(new Salle("Salle Concurrence", 20));
        Seance seance = seances.save(new Seance(film, salle,
                LocalDateTime.now().plusDays(3), new BigDecimal("9.90")));

        reservations.saveAndFlush(new Reservation("RES-PRE-1", seance,
                "pre1@example.test", 8, new BigDecimal("79.20")));
        reservations.saveAndFlush(new Reservation("RES-PRE-2", seance,
                "pre2@example.test", 7, new BigDecimal("69.30")));

        CyclicBarrier barriere = new CyclicBarrier(2);
        ExecutorService executeur = Executors.newFixedThreadPool(2);
        Callable<Integer> tache = () -> {
            barriere.await(5, TimeUnit.SECONDS);
            try {
                ResponseEntity<Map> reponse = rest.post()
                        .uri("/api/reservations")
                        .headers(headers -> headers.setBasicAuth("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(TestEntities.reservationPayload(seance.getId(),
                                "concurrent@example.test", 4))
                        .retrieve()
                        .toEntity(Map.class);
                return reponse.getStatusCode().value();
            } catch (RestClientResponseException ex) {
                return ex.getStatusCode().value();
            }
        };

        try {
            Future<Integer> premiere = executeur.submit(tache);
            Future<Integer> seconde = executeur.submit(tache);
            List<Integer> statuts = List.of(
                    premiere.get(30, TimeUnit.SECONDS),
                    seconde.get(30, TimeUnit.SECONDS));

            assertThat(statuts).containsExactlyInAnyOrder(201, 409);
            long placesConfirmees = reservations.sumPlacesBySeanceIdAndStatut(
                    seance.getId(), StatutReservation.CONFIRMEE);
            assertThat(placesConfirmees).isEqualTo(19);
            assertThat(placesConfirmees).isLessThanOrEqualTo(20);
        } finally {
            executeur.shutdownNow();
        }
    }

}
