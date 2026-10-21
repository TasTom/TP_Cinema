package fr.butinfo.cinema.ui;

import fr.butinfo.cinema.common.error.CapaciteInsuffisanteException;
import fr.butinfo.cinema.common.error.ConflitMetierException;
import fr.butinfo.cinema.common.error.RessourceIntrouvableException;
import fr.butinfo.cinema.config.SecurityConfig;
import fr.butinfo.cinema.film.CreerFilmCommand;
import fr.butinfo.cinema.film.FilmService;
import fr.butinfo.cinema.reservation.CreerReservationCommand;
import fr.butinfo.cinema.reservation.ReservationResult;
import fr.butinfo.cinema.reservation.ReservationService;
import fr.butinfo.cinema.reservation.StatutReservation;
import fr.butinfo.cinema.salle.SalleService;
import fr.butinfo.cinema.seance.SeanceResult;
import fr.butinfo.cinema.seance.SeanceService;
import fr.butinfo.cinema.security.JsonAccessDeniedHandler;
import fr.butinfo.cinema.security.JsonAuthenticationEntryPoint;
import fr.butinfo.cinema.support.TestEntities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {HomeController.class, CatalogueController.class, LoginController.class,
        ReservationUiController.class, AdminUiController.class, CatalogueImportController.class})
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        FormattingHelper.class, GlobalModelAttributes.class, UiExceptionHandler.class})
class UiMvcTest {

    @MockitoBean
    private fr.butinfo.cinema.catalogue.CatalogueExterneService catalogue;
    @MockitoBean
    private fr.butinfo.cinema.catalogue.ImportFilmService imports;

    @Test
    @WithMockUser(roles = "PROGRAMMATEUR")
    void afficheUneIndisponibiliteDuCatalogueSansCasserLaPage() throws Exception {
        when(catalogue.rechercher("GHIBLI", "")).thenThrow(new ConflitMetierException("CATALOGUE_INDISPONIBLE", "Catalogue indisponible"));
        mockMvc.perform(get("/admin/catalogue")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Catalogue indisponible")));
    }

    @Test
    @WithMockUser(roles = "PROGRAMMATEUR")
    void importeUnFilmAvecProtectionCsrfEtRedirigeVersSaFiche() throws Exception {
        when(imports.importer("TMDB", "42")).thenReturn(8L);
        mockMvc.perform(post("/admin/catalogue/importer").with(csrf()).param("source", "TMDB").param("id", "42"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/films/8"));
    }

    @Test
    @WithMockUser(roles = "PROGRAMMATEUR")
    void refuseUnImportSansCsrf() throws Exception {
        mockMvc.perform(post("/admin/catalogue/importer").param("source", "TMDB").param("id", "42"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(imports);
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void interditLeCatalogueExterneAuxClients() throws Exception {
        mockMvc.perform(get("/admin/catalogue")).andExpect(status().isForbidden());
        verifyNoInteractions(catalogue);
    }

    @Test
    void afficheLaFicheFilmEtSesSeances() throws Exception {
        when(films.trouver(1L)).thenReturn(TestEntities.film(1L, "Inception", 148));
        when(seances.rechercher(any(), any())).thenReturn(List.of(seance()));
        mockMvc.perform(get("/films/1")).andExpect(status().isOk()).andExpect(view().name("films/fiche"))
                .andExpect(content().string(containsString("Inception")));
    }

    private static final LocalDateTime DEBUT = LocalDateTime.of(2026, 11, 5, 20, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FilmService films;

    @MockitoBean
    private SalleService salles;

    @MockitoBean
    private SeanceService seances;

    @MockitoBean
    private ReservationService reservations;

    @ParameterizedTest
    @CsvSource({"/,index", "/films,films/index", "/seances,seances/index", "/login,login"})
    void afficheLesPagesPubliquesSansConnexion(String chemin, String vue) throws Exception {
        when(films.rechercher(null, null)).thenReturn(List.of(TestEntities.film(1L, "Inception", 148)));
        when(seances.rechercher(any(), any())).thenReturn(List.of(seance()));

        mockMvc.perform(get(chemin))
                .andExpect(status().isOk())
                .andExpect(view().name(vue))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("href=\"/login\"")))
                .andExpect(content().string(not(containsString("href=\"/admin\""))));
    }

    @Test
    void transmetLesFiltresDuCatalogueAuService() throws Exception {
        when(films.rechercher("Inception", "Science-fiction"))
                .thenReturn(List.of(TestEntities.film(1L, "Inception", 148)));

        mockMvc.perform(get("/films").param("titre", "Inception").param("genre", "Science-fiction"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Inception")));

        verify(films).rechercher("Inception", "Science-fiction");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/reservation/1", "/admin"})
    void redirigeUnVisiteurVersLaConnexion(String chemin) throws Exception {
        mockMvc.perform(get(chemin))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(films, salles, seances, reservations);
    }

    @Test
    @WithMockUser(username = "client", roles = "CLIENT")
    void afficheLeFormulaireAvecLesFormatsFrancaisEtUnJetonCsrf() throws Exception {
        when(seances.trouver(1L)).thenReturn(seance());

        mockMvc.perform(get("/reservation/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservation/form"))
                .andExpect(content().string(containsString("value=\"client@example.test\"")))
                .andExpect(resultat -> assertThat(resultat.getResponse().getContentAsString())
                        .containsPattern("name=\"_csrf\"[^>]+value=\"[^\"]+\""))
                .andExpect(content().string(containsString("05/11/2026 à 20:00")))
                .andExpect(content().string(containsString("9,90")))
                .andExpect(content().string(containsString("€")));
    }

    @Test
    @WithMockUser(username = "client", roles = "CLIENT")
    void creeUneReservationEtAfficheLaConfirmation() throws Exception {
        ReservationResult reservation = reservation();
        CreerReservationCommand commande = new CreerReservationCommand(1L, "client@example.test", 2);
        when(reservations.creer(commande)).thenReturn(reservation);

        MvcResult resultat = mockMvc.perform(post("/reservation").with(csrf())
                        .param("seanceId", "1").param("emailClient", "client@example.test")
                        .param("nombrePlaces", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservation/confirmation"))
                .andExpect(flash().attribute("reservation", reservation))
                .andReturn();

        verify(reservations).creer(commande);
        mockMvc.perform(get("/reservation/confirmation").flashAttrs(resultat.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(view().name("reservation/confirmation"))
                .andExpect(content().string(containsString("RES-TEST123456")))
                .andExpect(content().string(containsString("19,80")))
                .andExpect(content().string(containsString("€")));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void redirigeUneConfirmationSansReservationVersAccueil() throws Exception {
        mockMvc.perform(get("/reservation/confirmation"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verifyNoInteractions(reservations);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "9"})
    @WithMockUser(roles = "CLIENT")
    void refuseUnNombreDePlacesHorsLimitesSansCreerDeReservation(String places) throws Exception {
        when(seances.trouver(1L)).thenReturn(seance());

        mockMvc.perform(post("/reservation").with(csrf())
                        .param("seanceId", "1").param("emailClient", "client@example.test")
                        .param("nombrePlaces", places))
                .andExpect(status().isOk())
                .andExpect(view().name("reservation/form"))
                .andExpect(model().attributeHasFieldErrors("formulaire", "nombrePlaces"))
                .andExpect(content().string(containsString("Le nombre de places doit être compris entre 1 et 8.")));

        verifyNoInteractions(reservations);
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void afficheUnRefusDeCapaciteDansLeFormulaire() throws Exception {
        when(seances.trouver(1L)).thenReturn(seance());
        when(reservations.creer(any())).thenThrow(new CapaciteInsuffisanteException(1));

        mockMvc.perform(post("/reservation").with(csrf())
                        .param("seanceId", "1").param("emailClient", "client@example.test")
                        .param("nombrePlaces", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservation/form"))
                .andExpect(model().attributeHasFieldErrors("formulaire", "nombrePlaces"))
                .andExpect(content().string(containsString("place pour cette séance.")));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void redirigeUneReservationDontLaSeanceADisparu() throws Exception {
        when(reservations.creer(any())).thenThrow(new RessourceIntrouvableException(
                "SEANCE_INTROUVABLE", "La séance n'existe plus."));

        mockMvc.perform(post("/reservation").with(csrf())
                        .param("seanceId", "1").param("emailClient", "client@example.test")
                        .param("nombrePlaces", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seances"))
                .andExpect(flash().attribute("erreur", "La séance n'existe plus."));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void afficheUnePageHtmlPourUneSeanceIntrouvable() throws Exception {
        when(seances.trouver(999L)).thenThrow(new RessourceIntrouvableException(
                "SEANCE_INTROUVABLE", "La séance 999 n'existe pas."));

        mockMvc.perform(get("/reservation/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("erreur-metier"))
                .andExpect(model().attribute("code", "SEANCE_INTROUVABLE"))
                .andExpect(model().attribute("currentUser", "user"))
                .andExpect(model().attribute("estClient", true))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    void afficheUnePageHtmlPourUnParametreInvalideSansConnexion() throws Exception {
        mockMvc.perform(get("/seances").param("filmId", "invalide"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("erreur-metier"))
                .andExpect(model().attribute("code", "PARAMETRE_INVALIDE"))
                .andExpect(model().attribute("estAdmin", false))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("href=\"/login\"")));

        verifyNoInteractions(films, salles, seances, reservations);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/reservation", "/admin/films", "/admin/seances", "/admin/salles"})
    @WithMockUser(roles = "ADMIN")
    void refuseLesFormulairesSansJetonCsrf(String chemin) throws Exception {
        mockMvc.perform(post(chemin))
                .andExpect(status().isForbidden());

        verifyNoInteractions(films, salles, seances, reservations);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin", "/admin/films", "/admin/seances", "/admin/salles", "/admin/reservations"})
    @WithMockUser(roles = "CLIENT")
    void refuseLesPagesAdministrationAUnClient(String chemin) throws Exception {
        mockMvc.perform(get(chemin))
                .andExpect(status().isForbidden());

        verifyNoInteractions(films, salles, seances, reservations);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/salles", "/admin/reservations"})
    @WithMockUser(roles = "PROGRAMMATEUR")
    void refuseLesPagesReserveesAdminAUnProgrammateur(String chemin) throws Exception {
        mockMvc.perform(get(chemin))
                .andExpect(status().isForbidden());

        verifyNoInteractions(films, salles, seances, reservations);
    }

    @Test
    @WithMockUser(roles = "PROGRAMMATEUR")
    void autoriseLaCreationDeFilmAUnProgrammateur() throws Exception {
        mockMvc.perform(post("/admin/films").with(csrf())
                        .param("titre", "Inception").param("genre", "Science-fiction")
                        .param("dureeMinutes", "148"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/films"));

        verify(films).creer(new CreerFilmCommand("Inception", "Science-fiction", 148, null));
    }

    @ParameterizedTest
    @CsvSource({"/admin,admin/index", "/admin/films,admin/films", "/admin/seances,admin/seances",
            "/admin/salles,admin/salles", "/admin/reservations,admin/reservations"})
    @WithMockUser(username = "admin", roles = "ADMIN")
    void afficheLesPagesAdministrationAvecLeursDonnees(String chemin, String vue) throws Exception {
        when(films.rechercher(null, null)).thenReturn(List.of(TestEntities.film(1L, "Inception", 148)));
        when(salles.lister()).thenReturn(List.of(TestEntities.salle(1L, "Salle Lumière", 120)));
        when(seances.rechercher(null, null)).thenReturn(List.of(seance()));
        when(reservations.listerToutes()).thenReturn(List.of(reservation()));

        mockMvc.perform(get(chemin))
                .andExpect(status().isOk())
                .andExpect(view().name(vue))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("href=\"/admin\"")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void afficheUnConflitDeSalleSansPerdreLeFormulaire() throws Exception {
        when(salles.creer(any())).thenThrow(new ConflitMetierException(
                "SALLE_DEJA_EXISTANTE", "Une salle porte déjà ce nom."));

        mockMvc.perform(post("/admin/salles").with(csrf())
                        .param("nom", "Salle Lumière").param("capacite", "120"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/salles"))
                .andExpect(model().attributeHasErrors("salleForm"))
                .andExpect(resultat -> assertThat(HtmlUtils.htmlUnescape(
                        resultat.getResponse().getContentAsString())).contains("value=\"Salle Lumière\""))
                .andExpect(content().string(containsString("Une salle porte déjà ce nom.")));
    }

    private static SeanceResult seance() {
        return new SeanceResult(1L, 1L, "Inception", 1L, "Salle Lumière", DEBUT,
                new BigDecimal("9.90"), 120, 118);
    }

    private static ReservationResult reservation() {
        return new ReservationResult(1L, "RES-TEST123456", 1L, "Inception", "Salle Lumière", DEBUT,
                "client@example.test", 2, StatutReservation.CONFIRMEE, new BigDecimal("19.80"),
                LocalDateTime.of(2026, 10, 21, 12, 0));
    }
}
