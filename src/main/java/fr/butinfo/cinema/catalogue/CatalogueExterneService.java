package fr.butinfo.cinema.catalogue;

import fr.butinfo.cinema.common.error.DonneeInvalideException;
import fr.butinfo.cinema.common.error.ConflitMetierException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CatalogueExterneService {
    private final RestClient http;
    private final ObjectMapper json;
    private final String jeton;
    private final Map<String, EntreeCache> cache = new ConcurrentHashMap<>();

    @Autowired
    public CatalogueExterneService(ObjectMapper json, @Value("${cinema.tmdb.token:}") String jeton) {
        this(json, jeton, client());
    }

    CatalogueExterneService(ObjectMapper json, String jeton, RestClient http) {
        this.json = json;
        this.jeton = jeton;
        this.http = http;
    }

    private static RestClient client() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(8));
        return RestClient.builder().requestFactory(factory).build();
    }

    public boolean tmdbConfigure() { return !jeton.isBlank(); }

    public List<FilmExterne> rechercher(String source, String recherche) {
        String fournisseur = fournisseur(source);
        String terme = recherche == null ? "" : recherche.strip();
        if (terme.length() > 80) throw new DonneeInvalideException("RECHERCHE_TROP_LONGUE", "Limitez la recherche à 80 caractères.");
        String cle = fournisseur + (fournisseur.equals("TMDB") ? ":" + terme.toLowerCase(Locale.ROOT) : "");
        EntreeCache presente = cache.get(cle);
        List<FilmExterne> liste;
        if (presente != null && presente.expiration().isAfter(Instant.now())) {
            liste = presente.films();
        } else {
            try {
                liste = charger(fournisseur, terme);
                if (cache.size() >= 64) cache.clear();
                cache.put(cle, new EntreeCache(liste, Instant.now().plusSeconds(600)));
            } catch (RuntimeException ex) {
                throw new ConflitMetierException("CATALOGUE_INDISPONIBLE",
                        "Le catalogue externe est momentanément indisponible. Réessayez plus tard ; les films enregistrés restent accessibles.");
            }
        }
        if (fournisseur.equals("GHIBLI") && !terme.isBlank()) {
            return liste.stream().filter(f -> f.titre().toLowerCase(Locale.ROOT).contains(terme.toLowerCase(Locale.ROOT))).toList();
        }
        return liste;
    }

    public FilmExterne trouver(String source, String id) {
        String fournisseur = fournisseur(source);
        if (id == null || !id.matches(fournisseur.equals("TMDB") ? "[0-9]{1,12}" : "[a-f0-9-]{36}")) {
            throw new DonneeInvalideException("FILM_EXTERNE_INVALIDE", "L'identifiant du film n'est pas valide.");
        }
        if (fournisseur.equals("GHIBLI")) {
            return rechercher(fournisseur, "").stream().filter(f -> f.id().equals(id)).findFirst()
                    .orElseThrow(() -> new DonneeInvalideException("FILM_EXTERNE_INTROUVABLE", "Le film n'existe plus dans le catalogue."));
        }
        try {
            JsonNode film = lire(http.get().uri("https://api.themoviedb.org/3/movie/{id}?language=fr-FR", id)
                    .headers(h -> h.setBearerAuth(jeton)));
            return tmdb(film);
        } catch (RuntimeException ex) {
            throw new ConflitMetierException("CATALOGUE_INDISPONIBLE", "Impossible de récupérer ce film. Réessayez plus tard.");
        }
    }

    private List<FilmExterne> charger(String source, String terme) {
        JsonNode racine;
        if (source.equals("GHIBLI")) {
            racine = lire(http.get().uri("https://ghibliapi.vercel.app/films"));
        } else {
            racine = lire(http.get().uri(builder -> builder.scheme("https").host("api.themoviedb.org").path(
                            terme.isBlank() ? "/3/movie/popular" : "/3/search/movie")
                    .queryParam("language", "fr-FR").queryParam("include_adult", false)
                    .queryParam("page", 1).queryParam("query", terme).build())
                    .headers(h -> h.setBearerAuth(jeton)));
        }
        JsonNode films = source.equals("GHIBLI") ? racine : racine.path("results");
        if (!films.isArray()) throw new IllegalArgumentException("Catalogue invalide");
        List<FilmExterne> resultats = new ArrayList<>();
        for (JsonNode film : films) {
            if (resultats.size() == 100) break;
            FilmExterne resultat = source.equals("GHIBLI") ? ghibli(film) : tmdb(film);
            if (!resultat.titre().isBlank() && resultat.titre().length() <= 255) resultats.add(resultat);
        }
        return List.copyOf(resultats);
    }

    private JsonNode lire(RestClient.RequestHeadersSpec<?> requete) {
        return requete.exchange((demande, reponse) -> {
            if (!reponse.getStatusCode().is2xxSuccessful() || reponse.getHeaders().getContentLength() > 2_000_000) {
                throw new IllegalArgumentException("Réponse externe invalide");
            }
            byte[] octets = reponse.getBody().readNBytes(2_000_001);
            if (octets.length > 2_000_000) throw new IllegalArgumentException("Réponse externe trop longue");
            return json.readTree(new String(octets, StandardCharsets.UTF_8));
        });
    }

    private String fournisseur(String source) {
        String valeur = source == null ? (tmdbConfigure() ? "TMDB" : "GHIBLI") : source.toUpperCase(Locale.ROOT);
        if (!valeur.equals("TMDB") && !valeur.equals("GHIBLI")) throw new DonneeInvalideException("SOURCE_INVALIDE", "Choisissez TMDB ou Ghibli.");
        if (valeur.equals("TMDB") && !tmdbConfigure()) throw new DonneeInvalideException("TMDB_NON_CONFIGURE",
                "TMDB nécessite un jeton côté serveur. Utilisez Ghibli ou configurez TMDB_API_TOKEN.");
        return valeur;
    }

    private FilmExterne ghibli(JsonNode film) {
        String titre = film.path("title").asString("");
        String francais = switch (titre) {
            case "Spirited Away" -> "Le Voyage de Chihiro";
            case "My Neighbor Totoro" -> "Mon voisin Totoro";
            case "Castle in the Sky" -> "Le Château dans le ciel";
            case "Princess Mononoke" -> "Princesse Mononoké";
            case "Howl's Moving Castle" -> "Le Château ambulant";
            case "Kiki's Delivery Service" -> "Kiki la petite sorcière";
            case "Grave of the Fireflies" -> "Le Tombeau des lucioles";
            case "Whisper of the Heart" -> "Si tu tends l'oreille";
            case "The Wind Rises" -> "Le vent se lève";
            case "The Tale of the Princess Kaguya" -> "Le Conte de la princesse Kaguya";
            default -> titre;
        };
        return new FilmExterne(film.path("id").asString(""), francais,
                texte(film, "description", 10000), texte(film, "director", 255),
                entier(film.path("running_time").asString("0")), null,
                entier(film.path("release_date").asString("0")), imageGhibli(film, "image"),
                imageGhibli(film, "movie_banner"), "GHIBLI");
    }

    private FilmExterne tmdb(JsonNode film) {
        String date = film.path("release_date").asString("");
        LocalDate sortie = date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") ? LocalDate.parse(date) : null;
        return new FilmExterne(film.path("id").asString(""), texte(film, "title", 255),
                texte(film, "overview", 10000), "", film.path("runtime").asInt(0), sortie,
                sortie == null ? null : sortie.getYear(), imageTmdb(film, "poster_path", "w500"),
                imageTmdb(film, "backdrop_path", "w1280"), "TMDB");
    }

    private String imageTmdb(JsonNode film, String champ, String taille) {
        String chemin = film.path(champ).asString("");
        return chemin.matches("/[a-zA-Z0-9_-]+\\.(jpg|png|webp)") ? "https://image.tmdb.org/t/p/" + taille + chemin : "";
    }
    private String imageGhibli(JsonNode film, String champ) {
        String valeur = film.path(champ).asString("");
        return AffichesService.urlAutorisee(valeur) ? valeur : "";
    }
    private String texte(JsonNode node, String champ, int maximum) {
        String valeur = node.path(champ).asString("").strip();
        return valeur.substring(0, Math.min(valeur.length(), maximum));
    }
    private int entier(String valeur) { try { return Integer.parseInt(valeur); } catch (NumberFormatException ex) { return 0; } }
    private record EntreeCache(List<FilmExterne> films, Instant expiration) { }
}
