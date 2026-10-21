package fr.butinfo.cinema.catalogue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Service
public class AffichesService {
    private static final Logger log = LoggerFactory.getLogger(AffichesService.class);
    private static final int MAX_OCTETS = 5_000_000;
    private final Path repertoire;
    private final RestClient http;

    @Autowired
    public AffichesService(@Value("${cinema.media.directory:data/affiches}") String repertoire) {
        this(repertoire, client());
    }

    AffichesService(String repertoire, RestClient http) {
        this.repertoire = Path.of(repertoire).toAbsolutePath().normalize();
        this.http = http;
    }

    private static RestClient client() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(8));
        return RestClient.builder().requestFactory(factory).build();
    }

    public static boolean urlAutorisee(String valeur) {
        if (valeur == null || valeur.isBlank()) return false;
        try {
            URI uri = URI.create(valeur);
            return "https".equals(uri.getScheme()) && "image.tmdb.org".equals(uri.getHost())
                    && uri.getPort() == -1 && uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null
                    && uri.getPath().matches("/t/p/(w[0-9]+(_and_h[0-9]+_bestv2)?|original)/[A-Za-z0-9_-]+\\.(jpg|png|webp)");
        } catch (IllegalArgumentException ex) { return false; }
    }

    public String conserver(String adresse) {
        if (!urlAutorisee(adresse)) return null;
        try {
            String empreinte = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(adresse.getBytes(StandardCharsets.UTF_8)));
            for (String extension : new String[]{"jpg", "png", "webp"}) {
                if (Files.isRegularFile(repertoire.resolve(empreinte + "." + extension))) return "/affiches/" + empreinte + "." + extension;
            }
            return http.get().uri(URI.create(adresse)).exchange((demande, reponse) -> {
                if (!reponse.getStatusCode().is2xxSuccessful()) return null;
                String type = reponse.getHeaders().getContentType() == null ? "" : reponse.getHeaders().getContentType().toString();
                String extension = switch (type.split(";")[0]) {
                    case "image/jpeg" -> "jpg";
                    case "image/png" -> "png";
                    case "image/webp" -> "webp";
                    default -> null;
                };
                if (extension == null || reponse.getHeaders().getContentLength() > MAX_OCTETS) return null;
                byte[] octets = reponse.getBody().readNBytes(MAX_OCTETS + 1);
                if (octets.length > MAX_OCTETS || !signatureValide(octets, extension)) return null;
                Files.createDirectories(repertoire);
                String nom = empreinte + "." + extension;
                Files.write(repertoire.resolve(nom), octets);
                return "/affiches/" + nom;
            });
        } catch (RuntimeException | java.security.NoSuchAlgorithmException ex) {
            log.warn("Affiche distante indisponible ; le film reste importable sans image.");
            return null;
        }
    }

    public Path trouver(String nom) {
        if (!nom.matches("([a-f0-9-]{36}|[a-f0-9]{64})\\.(jpg|png|webp)")) return null;
        Path chemin = repertoire.resolve(nom).normalize();
        return chemin.startsWith(repertoire) && Files.isRegularFile(chemin) ? chemin : null;
    }

    private boolean signatureValide(byte[] b, String type) {
        if (b.length < 12) return false;
        return switch (type) {
            case "jpg" -> (b[0] & 255) == 255 && (b[1] & 255) == 216 && (b[2] & 255) == 255;
            case "png" -> b[0] == (byte)137 && b[1] == 80 && b[2] == 78 && b[3] == 71;
            case "webp" -> b[0] == 82 && b[1] == 73 && b[2] == 70 && b[3] == 70 && b[8] == 87 && b[9] == 69 && b[10] == 66 && b[11] == 80;
            default -> false;
        };
    }
}
