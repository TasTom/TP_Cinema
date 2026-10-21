package fr.butinfo.cinema;

import fr.butinfo.cinema.common.error.GlobalExceptionHandler;
import fr.butinfo.cinema.config.SecurityConfig;
import fr.butinfo.cinema.film.FilmService;
import fr.butinfo.cinema.film.api.FilmController;
import fr.butinfo.cinema.reservation.ReservationResult;
import fr.butinfo.cinema.reservation.ReservationService;
import fr.butinfo.cinema.reservation.StatutReservation;
import fr.butinfo.cinema.reservation.api.ReservationController;
import fr.butinfo.cinema.security.JsonAccessDeniedHandler;
import fr.butinfo.cinema.security.JsonAuthenticationEntryPoint;
import fr.butinfo.cinema.ui.GlobalModelAttributes;
import fr.butinfo.cinema.ui.LoginController;
import fr.butinfo.cinema.ui.UiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {FilmController.class, ReservationController.class, LoginController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class, GlobalModelAttributes.class, UiExceptionHandler.class})
class AuditSecuriteMvcTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private FilmService films;
    @MockitoBean private ReservationService reservations;

    @ParameterizedTest
    @CsvSource({"client,client123,201", "admin,admin123,201", "programmateur,prog123,403",
            "client,incorrect,401", "inconnu,incorrect,401"})
    void controleLesVraisIdentifiantsBcryptEtLesRoles(String compte, String motDePasse, int attendu) throws Exception {
        when(reservations.creer(any())).thenReturn(new ReservationResult(1L, "RES-AUDIT", 1L, "Audit", "Salle",
                LocalDateTime.of(2026, 11, 10, 20, 0), "audit@example.test", 1,
                StatutReservation.CONFIRMEE, new BigDecimal("9.90"), LocalDateTime.of(2026, 10, 21, 12, 0)));
        mvc.perform(post("/api/reservations").with(httpBasic(compte, motDePasse))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seanceId\":1,\"emailClient\":\"audit@example.test\",\"nombrePlaces\":1}"))
                .andExpect(status().is(attendu));
    }

    @Test
    void neReutilisePasLaSessionWebPourAuthentifierApi() throws Exception {
        MockHttpSession session = new MockHttpSession();
        var identite = UsernamePasswordAuthenticationToken.authenticated("admin", "inutilise",
                AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(identite));

        mvc.perform(post("/api/reservations").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seanceId\":1,\"emailClient\":\"audit@example.test\",\"nombrePlaces\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
        verifyNoInteractions(reservations);
    }

    @Test
    void refuseUneConnexionSansJetonCsrf() throws Exception {
        mvc.perform(post("/login").param("username", "client").param("password", "client123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void accepteLaConnexionAvecUnJetonCsrf() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username", "client").param("password", "client123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername("client"));
    }

    @Test
    void protegeLaDeconnexionParCsrf() throws Exception {
        mvc.perform(post("/logout").with(user("client").roles("CLIENT")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/logout").with(user("client").roles("CLIENT")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"))
                .andExpect(unauthenticated());
    }

    @Test
    void exposeLesLecturesAvecLesEntetesDeProtection() throws Exception {
        when(films.rechercher(null, null)).thenReturn(List.of());
        mvc.perform(get("/api/films"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void neDonnePasUneAutorisationCorsArbitraire() throws Exception {
        when(films.rechercher(null, null)).thenReturn(List.of());
        mvc.perform(get("/api/films").header("Origin", "https://origine-audit.invalid"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
