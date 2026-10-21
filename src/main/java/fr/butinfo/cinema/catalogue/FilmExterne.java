package fr.butinfo.cinema.catalogue;

import java.time.LocalDate;

public record FilmExterne(String id, String titre, String synopsis, String realisateur,
                          int dureeMinutes, LocalDate dateSortie, Integer annee,
                          String afficheUrl, String panoramaUrl, String source) { }
