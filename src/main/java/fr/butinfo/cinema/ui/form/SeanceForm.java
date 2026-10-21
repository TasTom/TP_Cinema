package fr.butinfo.cinema.ui.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SeanceForm {

    @NotNull(message = "Le film est obligatoire.")
    private Long filmId;

    @NotNull(message = "La salle est obligatoire.")
    private Long salleId;

    @NotNull(message = "La date et l'heure sont obligatoires.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime debut;

    @NotNull(message = "Le prix unitaire est obligatoire.")
    @DecimalMin(value = "0.01", message = "Le prix unitaire doit être strictement positif.")
    @Digits(integer = 8, fraction = 2, message = "Le prix unitaire doit comporter au maximum 8 chiffres entiers et 2 décimales.")
    private BigDecimal prixUnitaire;

    public Long getFilmId() {
        return filmId;
    }

    public void setFilmId(Long filmId) {
        this.filmId = filmId;
    }

    public Long getSalleId() {
        return salleId;
    }

    public void setSalleId(Long salleId) {
        this.salleId = salleId;
    }

    public LocalDateTime getDebut() {
        return debut;
    }

    public void setDebut(LocalDateTime debut) {
        this.debut = debut;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }
}
