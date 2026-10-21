package fr.butinfo.cinema.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<ChampErreur> details
) {

    public static ApiError of(HttpStatus status, String code, String message, String path) {
        return new ApiError(Instant.now(), status.value(), code, message, path, List.of());
    }

    public static ApiError of(HttpStatus status, String code, String message, String path,
                              List<ChampErreur> details) {
        return new ApiError(Instant.now(), status.value(), code, message, path, details);
    }
}
