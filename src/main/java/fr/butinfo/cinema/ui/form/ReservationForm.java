package fr.butinfo.cinema.ui.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReservationForm {

    @NotNull(message = "La séance est obligatoire.")
    private Long seanceId;

    @NotBlank(message = "L'adresse e-mail est obligatoire.")
    @Email(message = "L'adresse e-mail n'est pas valide.")
    @Size(max = 255, message = "L'adresse e-mail ne doit pas dépasser 255 caractères.")
    private String emailClient;

    @NotNull(message = "Le nombre de places est obligatoire.")
    @Min(value = 1, message = "Le nombre de places doit être compris entre 1 et 8.")
    @Max(value = 8, message = "Le nombre de places doit être compris entre 1 et 8.")
    private Integer nombrePlaces = 1;

    public ReservationForm() {
    }

    public ReservationForm(Long seanceId, String emailClient, Integer nombrePlaces) {
        this.seanceId = seanceId;
        this.emailClient = emailClient;
        this.nombrePlaces = nombrePlaces;
    }

    public Long getSeanceId() {
        return seanceId;
    }

    public void setSeanceId(Long seanceId) {
        this.seanceId = seanceId;
    }

    public String getEmailClient() {
        return emailClient;
    }

    public void setEmailClient(String emailClient) {
        this.emailClient = emailClient;
    }

    public Integer getNombrePlaces() {
        return nombrePlaces;
    }

    public void setNombrePlaces(Integer nombrePlaces) {
        this.nombrePlaces = nombrePlaces;
    }
}
