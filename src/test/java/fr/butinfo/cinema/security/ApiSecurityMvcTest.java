package fr.butinfo.cinema.security;

import fr.butinfo.cinema.common.error.ConflitMetierException;
import fr.butinfo.cinema.common.error.GlobalExceptionHandler;
import fr.butinfo.cinema.config.SecurityConfig;
import fr.butinfo.cinema.film.FilmService;
import fr.butinfo.cinema.film.api.FilmController;
import fr.butinfo.cinema.film.api.ProgrammationFilmController;
import fr.butinfo.cinema.salle.SalleService;
import fr.butinfo.cinema.salle.api.SalleAdminController;
import fr.butinfo.cinema.support.TestEntities;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {SalleAdminController.class, ProgrammationFilmController.class, FilmController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class,
        JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class})
class ApiSecurityMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SalleService salles;

    @MockitoBean
    private FilmService films;

    @Test
    void laLectureDesFilmsRestePublique() throws Exception {
        when(films.rechercher(null, null)).thenReturn(List.of(TestEntities.film(1L, "Inception", 148)));

        mockMvc.perform(get("/api/films"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titre").value("Inception"));
    }

    @Test
    void laCreationDeSalleEstRefuseeAnonymement() throws Exception {
        mockMvc.perform(post("/api/admin/salles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"Salle Test","capacite":100}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
    }

    @Test
    void laCreationDeSalleEstRefuseeAUnClient() throws Exception {
        mockMvc.perform(post("/api/admin/salles")
                        .with(user("client").roles("CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"Salle Test","capacite":100}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));

        verifyNoInteractions(salles);
    }

    @Test
    void laCreationDeSalleEstAutoriseeAUnAdmin() throws Exception {
        when(salles.creer(any())).thenReturn(TestEntities.salle(5L, "Salle Test", 100));

        mockMvc.perform(post("/api/admin/salles")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"Salle Test","capacite":100}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/salles/5")))
                .andExpect(jsonPath("$.nom").value("Salle Test"))
                .andExpect(jsonPath("$.capacite").value(100));
    }

    @Test
    void laCreationDeSalleValideLesDonnees() throws Exception {
        mockMvc.perform(post("/api/admin/salles")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"Salle Test","capacite":10}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"));

        verifyNoInteractions(salles);
    }

    @Test
    void uneSalleDupliqueeProduit409() throws Exception {
        when(salles.creer(any())).thenThrow(new ConflitMetierException("SALLE_DEJA_EXISTANTE",
                "Une salle nommée 'Salle Test' existe déjà."));

        mockMvc.perform(post("/api/admin/salles")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"Salle Test","capacite":100}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SALLE_DEJA_EXISTANTE"));
    }

    @Test
    void laProgrammationDeFilmEstRefuseeAUnClient() throws Exception {
        mockMvc.perform(post("/api/programmation/films")
                        .with(user("client").roles("CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titre":"Film","genre":"Drame","dureeMinutes":120}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(films);
    }

    @Test
    void laProgrammationDeFilmEstAutoriseeAUnProgrammateur() throws Exception {
        when(films.creer(any())).thenReturn(TestEntities.film(7L, "Film", 120));

        mockMvc.perform(post("/api/programmation/films")
                        .with(user("programmateur").roles("PROGRAMMATEUR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titre":"Film","genre":"Drame","dureeMinutes":120}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/films/7")))
                .andExpect(jsonPath("$.titre").value("Film"));
    }

    @Test
    void laProgrammationDeFilmEstAutoriseeAUnAdmin() throws Exception {
        when(films.creer(any())).thenReturn(TestEntities.film(8L, "Film admin", 120));

        mockMvc.perform(post("/api/programmation/films")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titre":"Film admin","genre":"Drame","dureeMinutes":120}
                                """))
                .andExpect(status().isCreated());
    }
}
