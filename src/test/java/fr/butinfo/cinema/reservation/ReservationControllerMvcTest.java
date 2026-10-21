package fr.butinfo.cinema.reservation;

import fr.butinfo.cinema.common.error.CapaciteInsuffisanteException;
import fr.butinfo.cinema.common.error.GlobalExceptionHandler;
import fr.butinfo.cinema.config.SecurityConfig;
import fr.butinfo.cinema.reservation.api.ReservationController;
import fr.butinfo.cinema.security.JsonAccessDeniedHandler;
import fr.butinfo.cinema.security.JsonAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReservationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class,
        JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class})
class ReservationControllerMvcTest {

    private static final String CORPS_VALIDE = """
            {"seanceId":1,"emailClient":"lea@example.test","nombrePlaces":3}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservations;

    @Test
    void creeUneReservationValide() throws Exception {
        when(reservations.creer(any())).thenReturn(resultat());

        mockMvc.perform(post("/api/reservations")
                        .with(user("client").roles("CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS_VALIDE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("RES-TEST"))
                .andExpect(jsonPath("$.statut").value("CONFIRMEE"))
                .andExpect(jsonPath("$.nombrePlaces").value(3))
                .andExpect(jsonPath("$.montantTotal").value(29.70));
    }

    @Test
    void refuseZeroPlaceAvecUnCorpsDErreurStable() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(user("client").roles("CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seanceId":1,"emailClient":"lea@example.test","nombrePlaces":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"))
                .andExpect(jsonPath("$.details[0].champ").value("nombrePlaces"));

        verifyNoInteractions(reservations);
    }

    @Test
    void refuseNeufPlacesAvecUnCorpsDErreurStable() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(user("client").roles("CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seanceId":1,"emailClient":"lea@example.test","nombrePlaces":9}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"));

        verifyNoInteractions(reservations);
    }

    @Test
    void exigeUneAuthentification() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS_VALIDE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
    }

    @Test
    void refuseUnRoleInsuffisant() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(user("programmateur").roles("PROGRAMMATEUR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS_VALIDE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
    }

    @Test
    void traduitLaCapaciteInsuffisanteEn409() throws Exception {
        when(reservations.creer(any())).thenThrow(new CapaciteInsuffisanteException(2));

        mockMvc.perform(post("/api/reservations")
                        .with(user("client").roles("CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS_VALIDE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAPACITE_INSUFFISANTE"))
                .andExpect(jsonPath("$.message").value("Il ne reste que 2 places pour cette séance."));
    }

    @Test
    void annuleUneReservation() throws Exception {
        mockMvc.perform(delete("/api/reservations/RES-TEST")
                        .with(user("client").roles("CLIENT")))
                .andExpect(status().isNoContent());

        verify(reservations).annuler("RES-TEST");
    }

    @Test
    void refuseLAnnulationAnonyme() throws Exception {
        mockMvc.perform(delete("/api/reservations/RES-TEST"))
                .andExpect(status().isUnauthorized());
    }

    private ReservationResult resultat() {
        return new ReservationResult(
                1L,
                "RES-TEST",
                1L,
                "Inception",
                "Salle Lumière",
                LocalDateTime.of(2026, 12, 1, 20, 0),
                "lea@example.test",
                3,
                StatutReservation.CONFIRMEE,
                new BigDecimal("29.70"),
                LocalDateTime.of(2026, 11, 1, 10, 0));
    }
}
