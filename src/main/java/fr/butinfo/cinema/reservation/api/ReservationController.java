package fr.butinfo.cinema.reservation.api;

import fr.butinfo.cinema.reservation.ReservationResult;
import fr.butinfo.cinema.reservation.ReservationService;
import fr.butinfo.cinema.reservation.api.dto.ReservationRequest;
import fr.butinfo.cinema.reservation.api.dto.ReservationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Réservations", description = "Création et annulation de réservations")
@SecurityRequirement(name = "basicAuth")
public class ReservationController {

    private final ReservationService reservations;

    public ReservationController(ReservationService reservations) {
        this.reservations = reservations;
    }

    @PostMapping
    @Operation(summary = "Créer une réservation",
            description = "Réservé aux rôles CLIENT et ADMIN. Le montant total est calculé côté serveur.")
    public ResponseEntity<ReservationResponse> creer(@Valid @RequestBody ReservationRequest request) {
        ReservationResult resultat = reservations.creer(request.versCommande());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReservationResponse.depuis(resultat));
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Annuler une réservation",
            description = "Réservé aux rôles CLIENT et ADMIN. Une annulation libère immédiatement les places.")
    public ResponseEntity<Void> annuler(@PathVariable String code) {
        reservations.annuler(code);
        return ResponseEntity.noContent().build();
    }
}
