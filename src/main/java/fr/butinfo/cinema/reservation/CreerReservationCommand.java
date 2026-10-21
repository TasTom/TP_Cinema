package fr.butinfo.cinema.reservation;

public record CreerReservationCommand(
        Long seanceId,
        String emailClient,
        int nombrePlaces
) {
}
