package fr.butinfo.cinema.reservation;

import fr.butinfo.cinema.common.error.CapaciteInsuffisanteException;
import fr.butinfo.cinema.common.error.DonneeInvalideException;
import fr.butinfo.cinema.common.error.RessourceIntrouvableException;
import fr.butinfo.cinema.film.Film;
import fr.butinfo.cinema.salle.Salle;
import fr.butinfo.cinema.seance.Seance;
import fr.butinfo.cinema.seance.SeanceRepository;
import fr.butinfo.cinema.support.TestEntities;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservations;

    @Mock
    private SeanceRepository seances;

    private SimpleMeterRegistry registry;
    private ReservationService service;
    private Film film;
    private Salle salle;
    private Seance seance;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        service = new ReservationService(reservations, seances, new ReservationMetrics(registry));
        film = TestEntities.film(1L, "Inception", 148);
        salle = TestEntities.salle(1L, "Salle Lumière", 10);
        seance = TestEntities.seance(1L, film, salle, LocalDateTime.of(2026, 12, 1, 20, 0), "9.90");
    }

    @Test
    void calculeLeMontantEtEnregistreLaReservation() {
        when(seances.findByIdForUpdate(1L)).thenReturn(Optional.of(seance));
        when(reservations.sumPlacesBySeanceIdAndStatut(1L, StatutReservation.CONFIRMEE)).thenReturn(0L);
        when(reservations.existsByCode(anyString())).thenReturn(false);
        when(reservations.saveAndFlush(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResult resultat = service.creer(
                new CreerReservationCommand(1L, "lea@example.test", 3));

        assertThat(resultat.code()).startsWith("RES-");
        assertThat(resultat.statut()).isEqualTo(StatutReservation.CONFIRMEE);
        assertThat(resultat.nombrePlaces()).isEqualTo(3);
        assertThat(resultat.montantTotal()).isEqualByComparingTo(new BigDecimal("29.70"));
        assertThat(resultat.filmTitre()).isEqualTo("Inception");
        assertThat(resultat.salleNom()).isEqualTo("Salle Lumière");
        assertThat(resultat.creeeLe()).isNotNull();

        assertThat(registry.get("cinema.reservations")
                .tag("resultat", "confirmee").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("cinema.places")
                .tag("resultat", "reservee").summary().totalAmount()).isEqualTo(3.0);
    }

    @Test
    void refuseUneReservationAuDelaDeLaCapacite() {
        when(seances.findByIdForUpdate(1L)).thenReturn(Optional.of(seance));
        when(reservations.sumPlacesBySeanceIdAndStatut(1L, StatutReservation.CONFIRMEE)).thenReturn(8L);

        assertThatThrownBy(() -> service.creer(
                new CreerReservationCommand(1L, "lea@example.test", 3)))
                .isInstanceOf(CapaciteInsuffisanteException.class)
                .hasMessageContaining("2 places");

        verify(reservations, never()).saveAndFlush(any(Reservation.class));
        assertThat(registry.get("cinema.reservations")
                .tag("resultat", "refusee").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("cinema.places")
                .tag("resultat", "refusee").summary().totalAmount()).isEqualTo(3.0);
    }

    @Test
    void annuleUneReservationEtLibereLesPlaces() {
        Reservation reservation = TestEntities.reservation(1L, "RES-ANNULE", seance, 3, "29.70");
        when(reservations.findByCode("RES-ANNULE")).thenReturn(Optional.of(reservation));
        when(seances.findByIdForUpdate(1L)).thenReturn(Optional.of(seance));

        service.annuler("RES-ANNULE");
        service.annuler("RES-ANNULE");

        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        assertThat(registry.get("cinema.reservations")
                .tag("resultat", "annulee").counter().count()).isEqualTo(1.0);
    }

    @Test
    void refuseUnNombreDePlacesInvalide() {
        assertThatThrownBy(() -> service.creer(
                new CreerReservationCommand(1L, "lea@example.test", 9)))
                .isInstanceOf(DonneeInvalideException.class)
                .hasMessageContaining("entre 1 et 8");

        verifyNoInteractions(seances, reservations);
    }

    @Test
    void signaleUneSeanceIntrouvable() {
        when(seances.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creer(
                new CreerReservationCommand(1L, "lea@example.test", 2)))
                .isInstanceOfSatisfying(RessourceIntrouvableException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("SEANCE_INTROUVABLE"));
    }
}
