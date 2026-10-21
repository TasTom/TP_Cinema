package fr.butinfo.cinema.ui.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SalleForm {

    @NotBlank(message = "Le nom de la salle est obligatoire.")
    @Size(max = 100, message = "Le nom de la salle ne doit pas dépasser 100 caractères.")
    private String nom;

    @NotNull(message = "La capacité est obligatoire.")
    @Min(value = 20, message = "La capacité doit être d'au moins 20 places.")
    @Max(value = 500, message = "La capacité ne doit pas dépasser 500 places.")
    private Integer capacite;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Integer getCapacite() {
        return capacite;
    }

    public void setCapacite(Integer capacite) {
        this.capacite = capacite;
    }
}
