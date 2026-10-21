package fr.butinfo.cinema.reservation.api.dto;

import fr.butinfo.cinema.reservation.CreerReservationCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReservationRequest(
        @NotNull(message = "La séance est obligatoire.")
        Long seanceId,

        @NotBlank(message = "L'adresse e-mail est obligatoire.")
        @Email(message = "L'adresse e-mail n'est pas valide.")
        @Size(max = 255, message = "L'adresse e-mail ne doit pas dépasser 255 caractères.")
        String emailClient,

        @NotNull(message = "Le nombre de places est obligatoire.")
        @Min(value = 1, message = "Le nombre de places doit être compris entre 1 et 8.")
        @Max(value = 8, message = "Le nombre de places doit être compris entre 1 et 8.")
        Integer nombrePlaces
) {

    public CreerReservationCommand versCommande() {
        return new CreerReservationCommand(seanceId, emailClient, nombrePlaces);
    }
}
