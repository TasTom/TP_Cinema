package fr.butinfo.cinema.salle.api;

import fr.butinfo.cinema.salle.Salle;
import fr.butinfo.cinema.salle.SalleService;
import fr.butinfo.cinema.salle.api.dto.SalleRequest;
import fr.butinfo.cinema.salle.api.dto.SalleResponse;
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
@RequestMapping("/api/admin/salles")
@Tag(name = "Administration des salles", description = "Création de salles réservée au rôle ADMIN")
@SecurityRequirement(name = "basicAuth")
public class SalleAdminController {

    private final SalleService salles;

    public SalleAdminController(SalleService salles) {
        this.salles = salles;
    }

    @PostMapping
    @Operation(summary = "Créer une salle", description = "Réservé au rôle ADMIN.")
    public ResponseEntity<SalleResponse> creer(@Valid @RequestBody SalleRequest request) {
        Salle salle = salles.creer(request.versCommande());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/salles/{id}")
                .buildAndExpand(salle.getId())
                .toUri();
        return ResponseEntity.created(location).body(SalleResponse.depuis(salle));
    }
}
