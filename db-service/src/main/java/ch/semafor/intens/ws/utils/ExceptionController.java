package ch.semafor.intens.ws.utils;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import io.swagger.v3.oas.annotations.Hidden;

@ControllerAdvice
@Hidden
public class ExceptionController {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionController.class);

    @ExceptionHandler(value = IntensWsException.class)
    public ResponseEntity<Object> exception(IntensWsException exception) {

        if(exception.getMessage() != null) {
            return new ResponseEntity<>(exception.getMessage(), exception.getStatus());
        } else if(exception.getData() != null ) {
            return new ResponseEntity<>(exception.getData(), exception.getStatus());
        }
        logger.warn("Exception thrown without message or data. StatusCode: {}", exception.getStatus());
        return new ResponseEntity<>("", exception.getStatus());
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<Object> exception(SQLException exception) {
        // Our DB user has no read permissions on the database
        // state codes starting with 42 are syntax or access rule violations
        if (exception.getSQLState().startsWith("42")) {
            return new ResponseEntity<>("DB is read only", HttpStatus.FORBIDDEN);
        }
        logger.error("Unhandled SQL Exception", exception);
        return new ResponseEntity<>("", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
