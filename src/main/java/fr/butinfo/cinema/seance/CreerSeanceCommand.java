package fr.butinfo.cinema.seance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreerSeanceCommand(
        Long filmId,
        Long salleId,
        LocalDateTime debut,
        BigDecimal prixUnitaire
) {
}
