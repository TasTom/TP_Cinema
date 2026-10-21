package fr.butinfo.cinema.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validationEchouee(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ChampErreur> details = Stream.concat(
                        ex.getBindingResult().getFieldErrors().stream()
                                .map(erreur -> new ChampErreur(erreur.getField(), erreur.getDefaultMessage())),
                        ex.getBindingResult().getGlobalErrors().stream()
                                .map(erreur -> new ChampErreur("_global", erreur.getDefaultMessage())))
                .distinct()
                .sorted(Comparator.comparing(ChampErreur::champ).thenComparing(ChampErreur::message))
                .toList();
        return reponse(HttpStatus.BAD_REQUEST, "VALIDATION_ECHOUEE",
                "Les données fournies sont invalides.", request, details);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> validationParametres(HandlerMethodValidationException ex, HttpServletRequest request) {
        return reponse(HttpStatus.BAD_REQUEST, "VALIDATION_ECHOUEE",
                "Les paramètres fournis sont invalides.", request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> violationContrainte(ConstraintViolationException ex, HttpServletRequest request) {
        List<ChampErreur> details = ex.getConstraintViolations().stream()
                .map(violation -> new ChampErreur(violation.getPropertyPath().toString(), violation.getMessage()))
                .sorted(Comparator.comparing(ChampErreur::champ).thenComparing(ChampErreur::message))
                .toList();
        return reponse(HttpStatus.BAD_REQUEST, "VALIDATION_ECHOUEE",
                "Les données fournies sont invalides.", request, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> requeteMalFormee(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return reponse(HttpStatus.BAD_REQUEST, "REQUETE_MAL_FORMEE",
                "Le corps de la requête est absent ou mal formé.", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiError> parametreManquant(MissingServletRequestParameterException ex,
                                               HttpServletRequest request) {
        return reponse(HttpStatus.BAD_REQUEST, "PARAMETRE_MANQUANT",
                "Le paramètre '" + ex.getParameterName() + "' est obligatoire.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> parametreInvalide(MethodArgumentTypeMismatchException ex,
                                               HttpServletRequest request) {
        return reponse(HttpStatus.BAD_REQUEST, "PARAMETRE_INVALIDE",
                "Le paramètre '" + ex.getName() + "' est invalide.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> methodeNonAutorisee(HttpRequestMethodNotSupportedException ex,
                                                 HttpServletRequest request) {
        return reponse(HttpStatus.METHOD_NOT_ALLOWED, "METHODE_NON_AUTORISEE",
                "La méthode HTTP utilisée n'est pas autorisée sur cette ressource.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> ressourceInconnue(NoResourceFoundException ex, HttpServletRequest request) {
        return reponse(HttpStatus.NOT_FOUND, "RESSOURCE_INTROUVABLE",
                "La ressource demandée n'existe pas.", request);
    }

    @ExceptionHandler(RessourceIntrouvableException.class)
    ResponseEntity<ApiError> ressourceIntrouvable(RessourceIntrouvableException ex, HttpServletRequest request) {
        return reponse(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(DonneeInvalideException.class)
    ResponseEntity<ApiError> donneeInvalide(DonneeInvalideException ex, HttpServletRequest request) {
        return reponse(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler({ConflitMetierException.class, CapaciteInsuffisanteException.class})
    ResponseEntity<ApiError> conflit(MetierException ex, HttpServletRequest request) {
        return reponse(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflitDonnees(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.debug("Conflit d'intégrité en base", ex);
        return reponse(HttpStatus.CONFLICT, "CONFLIT_DONNEES",
                "Un conflit de données empêche l'opération.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> accesRefuse(AccessDeniedException ex, HttpServletRequest request) {
        return reponse(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                "Vous n'avez pas les droits nécessaires pour cette opération.", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> erreurInterne(Exception ex, HttpServletRequest request) {
        log.error("Erreur interne non gérée sur {}", request.getRequestURI(), ex);
        return reponse(HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE",
                "Une erreur interne est survenue.", request);
    }

    private ResponseEntity<ApiError> reponse(HttpStatus status, String code, String message,
                                             HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status, code, message, request.getRequestURI()));
    }

    private ResponseEntity<ApiError> reponse(HttpStatus status, String code, String message,
                                             HttpServletRequest request, List<ChampErreur> details) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status, code, message, request.getRequestURI(), details));
    }
}
