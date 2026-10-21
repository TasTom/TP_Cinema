package fr.butinfo.cinema.ui.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class FilmForm {

    @NotBlank(message = "Le titre est obligatoire.")
    @Size(max = 255, message = "Le titre ne doit pas dépasser 255 caractères.")
    private String titre;

    @Size(max = 80, message = "Le genre ne doit pas dépasser 80 caractères.")
    private String genre;

    @NotNull(message = "La durée est obligatoire.")
    @Min(value = 40, message = "La durée doit être d'au moins 40 minutes.")
    @Max(value = 300, message = "La durée ne doit pas dépasser 300 minutes.")
    private Integer dureeMinutes;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateSortie;

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getDureeMinutes() {
        return dureeMinutes;
    }

    public void setDureeMinutes(Integer dureeMinutes) {
        this.dureeMinutes = dureeMinutes;
    }

    public LocalDate getDateSortie() {
        return dateSortie;
    }

    public void setDateSortie(LocalDate dateSortie) {
        this.dateSortie = dateSortie;
    }
}
