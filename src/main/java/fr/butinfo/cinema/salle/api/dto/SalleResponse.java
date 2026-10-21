package fr.butinfo.cinema.salle.api.dto;

import fr.butinfo.cinema.salle.Salle;

public record SalleResponse(Long id, String nom, int capacite) {

    public static SalleResponse depuis(Salle salle) {
        return new SalleResponse(salle.getId(), salle.getNom(), salle.getCapacite());
    }
}
