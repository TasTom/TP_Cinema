package fr.butinfo.cinema.salle.api;

import fr.butinfo.cinema.salle.SalleService;
import fr.butinfo.cinema.salle.api.dto.SalleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/salles")
@Tag(name = "Salles", description = "Salles de cinéma (lecture publique)")
public class SalleController {

    private final SalleService salles;

    public SalleController(SalleService salles) {
        this.salles = salles;
    }

    @GetMapping
    @Operation(summary = "Lister les salles")
    public List<SalleResponse> lister() {
        return salles.lister().stream()
                .map(SalleResponse::depuis)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une salle")
    public SalleResponse trouver(@PathVariable Long id) {
        return SalleResponse.depuis(salles.trouver(id));
    }
}
