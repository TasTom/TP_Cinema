package fr.butinfo.cinema.ui;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component("format")
public class FormattingHelper {

    private static final Locale FRANCAIS = Locale.FRENCH;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", FRANCAIS);
    private static final DateTimeFormatter DATE_COMPLETE =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", FRANCAIS);
    private static final DateTimeFormatter DATE_HEURE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", FRANCAIS);
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm", FRANCAIS);

    public String date(LocalDate valeur) {
        return valeur == null ? "" : DATE.format(valeur);
    }

    public String jour(LocalDateTime valeur) {
        return valeur == null ? "" : capitaliser(DATE_COMPLETE.format(valeur));
    }

    public String dateHeure(LocalDateTime valeur) {
        return valeur == null ? "" : DATE_HEURE.format(valeur);
    }

    public String heure(LocalDateTime valeur) {
        return valeur == null ? "" : HEURE.format(valeur);
    }

    public String monnaie(BigDecimal valeur) {
        return valeur == null ? "" : NumberFormat.getCurrencyInstance(Locale.FRANCE).format(valeur);
    }

    private static String capitaliser(String valeur) {
        if (valeur == null || valeur.isEmpty()) {
            return valeur;
        }
        return Character.toUpperCase(valeur.charAt(0)) + valeur.substring(1);
    }
}
