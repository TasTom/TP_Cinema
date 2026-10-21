package fr.butinfo.cinema.support;

import fr.butinfo.cinema.film.Film;
import fr.butinfo.cinema.reservation.Reservation;
import fr.butinfo.cinema.salle.Salle;
import fr.butinfo.cinema.seance.Seance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public final class TestEntities {

    private TestEntities() {
    }

    public static Film film(long id, String titre, int dureeMinutes) {
        Film film = new Film(titre, "Drame", dureeMinutes, LocalDate.of(2026, 1, 1));
        setId(film, id);
        return film;
    }

    public static Salle salle(long id, String nom, int capacite) {
        Salle salle = new Salle(nom, capacite);
        setId(salle, id);
        return salle;
    }

    public static Seance seance(long id, Film film, Salle salle, LocalDateTime debut, String prix) {
        Seance seance = new Seance(film, salle, debut, new BigDecimal(prix));
        setId(seance, id);
        return seance;
    }

    public static Reservation reservation(long id, String code, Seance seance, int places, String total) {
        Reservation reservation = new Reservation(code, seance, "client@example.test", places,
                new BigDecimal(total));
        setId(reservation, id);
        return reservation;
    }

    public static void setId(Object entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    public static Map<String, Object> reservationPayload(long seanceId, String emailClient, int nombrePlaces) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("seanceId", seanceId);
        payload.put("emailClient", emailClient);
        payload.put("nombrePlaces", nombrePlaces);
        return payload;
    }

    public static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
