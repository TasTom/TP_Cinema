package fr.butinfo.cinema;

import fr.butinfo.cinema.film.Film;
import fr.butinfo.cinema.film.FilmRepository;
import fr.butinfo.cinema.reservation.CreerReservationCommand;
import fr.butinfo.cinema.reservation.ReservationRepository;
import fr.butinfo.cinema.reservation.ReservationService;
import fr.butinfo.cinema.salle.Salle;
import fr.butinfo.cinema.salle.SalleRepository;
import fr.butinfo.cinema.seance.Seance;
import fr.butinfo.cinema.seance.SeanceRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.hikari.connection-init-sql=SET SESSION innodb_lock_wait_timeout=6",
        "logging.level.root=WARN"})
@Testcontainers(disabledWithoutDocker = true)
class AuditCasLimitesTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:9.7");

    @LocalServerPort private int port;
    @Autowired private FilmRepository films;
    @Autowired private SalleRepository salles;
    @Autowired private SeanceRepository seances;
    @Autowired private ReservationRepository reservations;
    @Autowired private ReservationService service;
    @Autowired private MeterRegistry metriques;
    @Autowired private DataSource source;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private UserDetailsService utilisateurs;
    @Autowired private ObjectMapper json;
    private RestClient rest;
    private Seance seance;

    @BeforeEach
    void preparerDesDonneesIsolees() {
        rest = RestClient.create();
        reservations.deleteAll();
        seances.deleteAll();
        salles.deleteAll();
        films.deleteAll();
        Film film = films.save(new Film("Audit", "Test", 120, LocalDate.of(2026, 1, 1)));
        Salle salle = salles.save(new Salle("Salle Audit", 20));
        seance = seances.save(new Seance(film, salle, LocalDateTime.of(2026, 11, 10, 20, 0),
                new BigDecimal("9.90")));
    }

    @Test
    void refuseUneQuantiteDecimaleSansLaTronquer() {
        Reponse reponse = appeler("POST", "/api/reservations", "client", payload("1.9"), "application/json", null);
        observer("quantite-decimale", reponse);
        assertThat(reponse.statut()).isEqualTo(400);
    }

    @Test
    void refuseUnIdentifiantDecimalSansCiblerUneAutreSeance() {
        String corps = "{\"seanceId\":" + seance.getId() + ".9,\"emailClient\":\"audit@example.test\",\"nombrePlaces\":1}";
        Reponse reponse = appeler("POST", "/api/reservations", "client", corps, "application/json", null);
        observer("identifiant-decimal", reponse);
        assertThat(reponse.statut()).isEqualTo(400);
    }

    @Test
    void reserveHuitPlacesPourUnPrixAutoriseParLeDto() {
        Reponse creation = appeler("POST", "/api/programmation/seances", "programmateur",
                "{\"filmId\":" + seance.getFilm().getId() + ",\"salleId\":" + seance.getSalle().getId()
                        + ",\"debut\":\"2026-11-11T20:00:00\",\"prixUnitaire\":99999999.99}", "application/json", null);
        assertThat(creation.statut()).isEqualTo(201);
        long id = json.readTree(creation.corps()).path("id").asLong();
        Reponse reponse = appeler("POST", "/api/reservations", "client",
                "{\"seanceId\":" + id + ",\"emailClient\":\"audit@example.test\",\"nombrePlaces\":8}", "application/json", null);
        observer("montant-maximal", reponse);
        assertThat(reservations.count()).isEqualTo(reponse.statut() == 201 ? 1 : 0);
        assertThat(reponse.statut()).isEqualTo(201);
    }

    @Test
    void renvoie415PourUnTypeDeCorpsNonPrisEnCharge() {
        Reponse reponse = appeler("POST", "/api/reservations", "client", payload("1"), "text/plain", null);
        observer("content-type", reponse);
        assertThat(reponse.statut()).isEqualTo(415);
    }

    @Test
    void renvoie406PourUnFormatDeReponseNonPrisEnCharge() {
        Reponse reponse = appeler("GET", "/api/films", null, null, null, "application/xml");
        observer("accept", reponse);
        assertThat(reponse.statut()).isEqualTo(406);
    }

    @Test
    void conserveUnCodeStableSurUneRouteApiInconnue() {
        Reponse reponse = appeler("GET", "/api/absent", "client", null, null, null);
        observer("route-inconnue", reponse);
        assertThat(reponse.statut()).isEqualTo(404);
        assertThat(json.readTree(reponse.corps()).path("code").asText()).isEqualTo("RESSOURCE_INTROUVABLE");
    }

    @Test
    void renvoie405PourUneMethodeNonAutorisee() {
        Reponse reponse = appeler("PUT", "/api/films", "client", "{}", "application/json", null);
        observer("methode-inconnue", reponse);
        assertThat(reponse.statut()).isEqualTo(405);
    }

    @Test
    void refuseUneDateExtremeAvecUneErreurClient() {
        Reponse reponse = appeler("GET", "/api/seances?au=%2B999999999-12-31", null, null, null, null);
        observer("date-extreme", reponse);
        assertThat(reponse.statut()).isEqualTo(400);
    }

    @Test
    void nePubliePasLesMotsDePasseDansLaPageDeConnexion() {
        Reponse reponse = appeler("GET", "/login", null, null, null, null);
        System.out.println("AUDIT|mots-de-passe-page-login|admin123=" + reponse.corps().contains("admin123"));
        assertThat(reponse.corps()).doesNotContain("admin123", "client123", "prog123");
    }

    @Test
    void ignoreUnMontantEtUnStatutFournisParLeClient() {
        String corps = payload("3").replace("}", ",\"montantTotal\":0.01,\"statut\":\"ANNULEE\"}");
        Reponse reponse = appeler("POST", "/api/reservations", "client", corps, "application/json", null);
        observer("champs-metier-injectes", reponse);
        assertThat(reponse.statut()).isEqualTo(201);
        assertThat(json.readTree(reponse.corps()).path("montantTotal").asDouble()).isEqualTo(29.70);
        assertThat(json.readTree(reponse.corps()).path("statut").asText()).isEqualTo("CONFIRMEE");
    }

    @Test
    void echappeUnTitreHtmlDansLeCatalogue() {
        films.save(new Film("<script>alert('audit')</script>", "Test", 120, null));
        Reponse reponse = appeler("GET", "/films", null, null, null, null);
        System.out.println("AUDIT|xss-stockee|http=" + reponse.statut());
        assertThat(reponse.statut()).isEqualTo(200);
        assertThat(reponse.corps()).doesNotContain("<script>alert('audit')</script>").contains("&lt;script&gt;");
    }

    @Test
    void constateLAnnulationSansLienAvecLeCompteCreateur() {
        InMemoryUserDetailsManager gestionnaire = (InMemoryUserDetailsManager) utilisateurs;
        if (!gestionnaire.userExists("autre")) {
            gestionnaire.createUser(User.withUsername("autre").password(gestionnaire.loadUserByUsername("client").getPassword())
                    .roles("CLIENT").build());
        }
        Reponse creation = appeler("POST", "/api/reservations", "client", payload("2"), "application/json", null);
        String code = json.readTree(creation.corps()).path("code").asText();
        Reponse reponse = appeler("DELETE", "/api/reservations/" + code, "autre", null, null, null);
        observer("annulation-autre-client", reponse);
        assertThat(reponse.statut()).isEqualTo(204);
    }

    @Test
    void transformeUnConflitDeVerrouEn409SansEcriture() throws Exception {
        try (Connection verrou = verrouillerSeance()) {
            Reponse reponse = appeler("POST", "/api/reservations", "client", payload("2"), "application/json", null);
            observer("expiration-verrou", reponse);
            verrou.rollback();
            assertThat(reservations.count()).isZero();
            assertThat(reponse.statut()).isEqualTo(409);
        }
    }

    @Test
    void neCompteUneAnnulationConcurrenteQuUneSeuleFois() throws Exception {
        String code = service.creer(new CreerReservationCommand(seance.getId(), "audit@example.test", 2)).code();
        double avant = metriques.get("cinema.reservations").tag("resultat", "annulee").counter().count();
        var executeur = Executors.newFixedThreadPool(2);
        try (Connection verrou = verrouillerSeance()) {
            CyclicBarrier barriere = new CyclicBarrier(3);
            var premiere = executeur.submit(() -> {
                barriere.await(5, TimeUnit.SECONDS);
                return appeler("DELETE", "/api/reservations/" + code, "client", null, null, null);
            });
            var seconde = executeur.submit(() -> {
                barriere.await(5, TimeUnit.SECONDS);
                return appeler("DELETE", "/api/reservations/" + code, "client", null, null, null);
            });
            barriere.await(5, TimeUnit.SECONDS);
            try (Connection controle = DriverManager.getConnection(mysql.getJdbcUrl(), "root", mysql.getPassword())) {
                long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
                int attentes = 0;
                while (System.nanoTime() < limite) {
                    try (var requete = controle.createStatement(); var resultat = requete.executeQuery(
                            "SELECT COUNT(*) FROM performance_schema.data_lock_waits")) {
                        resultat.next(); attentes = resultat.getInt(1);
                    }
                    if (attentes >= 2) break;
                    Thread.sleep(20);
                }
                assertThat(attentes).as("les deux annulations ont lu puis attendent le verrou").isGreaterThanOrEqualTo(2);
            } finally {
                verrou.rollback();
            }
            assertThat(premiere.get(15, TimeUnit.SECONDS).statut()).isEqualTo(204);
            assertThat(seconde.get(15, TimeUnit.SECONDS).statut()).isEqualTo(204);
            double delta = metriques.get("cinema.reservations").tag("resultat", "annulee").counter().count() - avant;
            System.out.println("AUDIT|double-annulation|delta-metrique=" + delta);
            assertThat(delta).isEqualTo(1.0);
        } finally {
            executeur.shutdownNow();
        }
    }

    @Test
    void neComptePasUneReservationDontLaTransactionEstAnnulee() {
        double avant = metriques.get("cinema.reservations").tag("resultat", "confirmee").counter().count();
        new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
            service.creer(new CreerReservationCommand(seance.getId(), "audit@example.test", 2));
            transaction.setRollbackOnly();
        });
        assertThat(reservations.count()).isZero();
        double delta = metriques.get("cinema.reservations").tag("resultat", "confirmee").counter().count() - avant;
        System.out.println("AUDIT|rollback|delta-metrique=" + delta);
        assertThat(delta).isZero();
    }

    private Connection verrouillerSeance() throws Exception {
        Connection verrou = source.getConnection();
        verrou.setAutoCommit(false);
        try (var requete = verrou.createStatement(); var resultat = requete.executeQuery(
                "SELECT id FROM seances WHERE id=" + seance.getId() + " FOR UPDATE")) {
            assertThat(resultat.next()).isTrue();
        }
        return verrou;
    }

    @Test
    void reutiliseLesPlacesAnnuleesSansLesLibererDeuxFois() {
        var premiere = service.creer(new CreerReservationCommand(seance.getId(), "audit@example.test", 8));
        service.creer(new CreerReservationCommand(seance.getId(), "audit@example.test", 8));
        service.creer(new CreerReservationCommand(seance.getId(), "audit@example.test", 4));
        service.annuler(premiere.code());
        service.creer(new CreerReservationCommand(seance.getId(), "audit@example.test", 8));
        service.annuler(premiere.code());
        Reponse reponse = appeler("POST", "/api/reservations", "client", payload("1"), "application/json", null);
        observer("reutilisation-apres-annulation", reponse);
        assertThat(reponse.statut()).isEqualTo(409);
        assertThat(reservations.sumPlacesBySeanceIdAndStatut(seance.getId(),
                fr.butinfo.cinema.reservation.StatutReservation.CONFIRMEE)).isEqualTo(20);
    }

    @Test
    void appliqueLesContraintesSqlSurUneBaseReelle() throws Exception {
        try (Connection connexion = source.getConnection(); var requete = connexion.createStatement()) {
            assertThatThrownBy(() -> requete.executeUpdate("INSERT INTO salles(nom, capacite) VALUES ('Salle Audit', 20)"))
                    .isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(() -> requete.executeUpdate("INSERT INTO films(titre,duree_minutes) VALUES ('Invalide',39)"))
                    .isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(() -> requete.executeUpdate("INSERT INTO seances(film_id,salle_id,debut,prix_unitaire) VALUES ("
                    + seance.getFilm().getId() + "," + seance.getSalle().getId() + ",'2026-11-10 20:00:00',0)"))
                    .isInstanceOf(java.sql.SQLException.class);
        }
        System.out.println("AUDIT|contraintes-sql|unicite-salle+duree-film+prix-positif=OK");
    }

    private String payload(String places) {
        return "{\"seanceId\":" + seance.getId() + ",\"emailClient\":\"audit@example.test\",\"nombrePlaces\":" + places + "}";
    }

    private Reponse appeler(String methode, String chemin, String utilisateur, String corps, String type, String accept) {
        var requete = rest.method(HttpMethod.valueOf(methode)).uri(URI.create("http://localhost:" + port + chemin));
        if (utilisateur != null) {
            String motDePasse = utilisateur.equals("programmateur") ? "prog123" : utilisateur.equals("autre") ? "client123" : utilisateur + "123";
            requete.headers(entetes -> entetes.setBasicAuth(utilisateur, motDePasse));
        }
        if (type != null) requete.contentType(MediaType.parseMediaType(type));
        if (accept != null) requete.header("Accept", accept);
        if (corps != null) requete.body(corps);
        return requete.exchange((demande, reponse) -> new Reponse(reponse.getStatusCode().value(),
                new String(reponse.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    private void observer(String nom, Reponse reponse) {
        System.out.println("AUDIT|" + nom + "|http=" + reponse.statut() + "|" + reponse.corps());
    }

    private record Reponse(int statut, String corps) { }
}
