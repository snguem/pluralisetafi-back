package pluralisconseil.sn.pluralisEtatFin.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class ExceptionInterceptor extends ResponseEntityExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public final ResponseEntity<Object> handleEntityNotFoundException(NotFoundException exception){
       return new ResponseEntity<>(exception.getMessage(), HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(FileNotFoundException.class)
    public final ResponseEntity<Object> handleFileNotFoundException(FileNotFoundException exception){
       return new ResponseEntity<>(exception.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public final ResponseEntity<Object> handleBadRequestException(BadRequestException exception){
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(EntrepriseLockedException.class)
    public final ResponseEntity<Object> handleEntrepriseLockedException(EntrepriseLockedException exception){
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.FORBIDDEN);
    }
    @ExceptionHandler(UnauthorizeException.class)
    public final ResponseEntity<Object> handleUnauthorizeException(UnauthorizeException exception){
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodNotAllowedException.class)
    public final ResponseEntity<Object> handleMethodNotAllowedException(MethodNotAllowedException exception){
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.METHOD_NOT_ALLOWED);
    }

    /** Anything not handled above: the full stack trace goes to the server log, the client gets a short 500. */
    @ExceptionHandler(Exception.class)
    public final ResponseEntity<Object> handleUnexpected(Exception exception){
        log.error("Erreur non gérée", exception);
        return new ResponseEntity<>("Une erreur est survenue, consultez les logs du serveur", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
