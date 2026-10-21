package fr.butinfo.cinema.reservation;

import fr.butinfo.cinema.film.Film;
import fr.butinfo.cinema.film.FilmRepository;
import fr.butinfo.cinema.salle.Salle;
import fr.butinfo.cinema.salle.SalleRepository;
import fr.butinfo.cinema.seance.Seance;
import fr.butinfo.cinema.seance.SeanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ReservationRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:9.7");

    @Autowired
    private ReservationRepository reservations;

    @Autowired
    private SeanceRepository seances;

    @Autowired
    private FilmRepository films;

    @Autowired
    private SalleRepository salles;

    @Test
    void sommeLesPlacesConfirmeesEtLibereApresAnnulation() {
        Film film = films.findById(1L).orElseThrow();
        Salle salle = salles.findById(1L).orElseThrow();
        Seance seance = seances.save(new Seance(film, salle,
                LocalDateTime.of(2026, 12, 10, 20, 0), new BigDecimal("9.90")));

        Reservation premiere = reservations.saveAndFlush(new Reservation(
                "RES-JPA-1", seance, "a@example.test", 3, new BigDecimal("29.70")));
        reservations.saveAndFlush(new Reservation(
                "RES-JPA-2", seance, "b@example.test", 2, new BigDecimal("19.80")));

        assertThat(reservations.sumPlacesBySeanceIdAndStatut(seance.getId(), StatutReservation.CONFIRMEE))
                .isEqualTo(5);

        premiere.annuler();
        reservations.saveAndFlush(premiere);

        assertThat(reservations.sumPlacesBySeanceIdAndStatut(seance.getId(), StatutReservation.CONFIRMEE))
                .isEqualTo(2);
        assertThat(reservations.sumPlacesParSeance(StatutReservation.CONFIRMEE))
                .anySatisfy(projection -> {
                    assertThat(projection.getSeanceId()).isEqualTo(seance.getId());
                    assertThat(projection.getPlaces()).isEqualTo(2L);
                });
    }
}
