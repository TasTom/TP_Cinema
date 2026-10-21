package fr.butinfo.cinema.seance.api;

import fr.butinfo.cinema.seance.SeanceResult;
import fr.butinfo.cinema.seance.SeanceService;
import fr.butinfo.cinema.seance.api.dto.SeanceRequest;
import fr.butinfo.cinema.seance.api.dto.SeanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/programmation/seances")
@Tag(name = "Programmation des séances", description = "Création de séances réservée aux rôles PROGRAMMATEUR et ADMIN")
@SecurityRequirement(name = "basicAuth")
public class ProgrammationSeanceController {

    private final SeanceService seances;

    public ProgrammationSeanceController(SeanceService seances) {
        this.seances = seances;
    }

    @PostMapping
    @Operation(summary = "Créer une séance", description = "Réservé aux rôles PROGRAMMATEUR et ADMIN.")
    public ResponseEntity<SeanceResponse> creer(@Valid @RequestBody SeanceRequest request) {
        SeanceResult seance = seances.creer(request.versCommande());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/seances/{id}")
                .buildAndExpand(seance.id())
                .toUri();
        return ResponseEntity.created(location).body(SeanceResponse.depuis(seance));
    }
}
