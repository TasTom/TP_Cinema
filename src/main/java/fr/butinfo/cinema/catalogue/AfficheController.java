package fr.butinfo.cinema.catalogue;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.time.Duration;

@RestController
public class AfficheController {
    private final AffichesService affiches;
    public AfficheController(AffichesService affiches) { this.affiches = affiches; }

    @GetMapping("/affiches/{nom}")
    public ResponseEntity<Resource> affiche(@PathVariable String nom) {
        var chemin = affiches.trouver(nom);
        if (chemin == null) return ResponseEntity.notFound().build();
        MediaType type = nom.endsWith("png") ? MediaType.IMAGE_PNG : nom.endsWith("webp")
                ? MediaType.parseMediaType("image/webp") : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(new FileSystemResource(chemin));
    }
}
