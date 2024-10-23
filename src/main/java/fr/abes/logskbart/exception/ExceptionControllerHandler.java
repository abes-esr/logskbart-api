package fr.abes.logskbart.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.text.ParseException;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ExceptionControllerHandler extends ResponseEntityExceptionHandler {
    private ResponseEntity<Object> buildResponseEntity(ApiReturnError apiReturnError) {
        return new ResponseEntity<>(apiReturnError, apiReturnError.getStatus());
    }

    @ExceptionHandler(EmptyFileException.class)
    protected ResponseEntity<Object> handleEmptyFileException(EmptyFileException e) {
        return buildResponseEntity(new ApiReturnError(HttpStatus.NOT_FOUND, e.getMessage()));
    }
    /**
     * Erreur dans la validité des paramètres de la requête
     *
     * @param ex : l'exception catchée
     * @return l'objet du message d'erreur
     */
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
        String error = "Erreur dans les paramètres de la requête";
        log.debug(ex.getLocalizedMessage());
        return buildResponseEntity(new ApiReturnError(HttpStatus.BAD_REQUEST, error, ex));
    }

    @ExceptionHandler(ParseException.class)
    protected ResponseEntity<Object> handleParseException(ParseException ex) {
        String error = "Erreur dans le format de la date : format attendu : ddMMyyyy";
        log.debug(ex.getLocalizedMessage());
        return buildResponseEntity(new ApiReturnError(HttpStatus.BAD_REQUEST, error, ex));
    }
}
