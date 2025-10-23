package ch.semafor.gendas.exceptions;

/**
 * Created by mau on 5/12/16.
 */
public class GroupExistsException extends Exception {

    public GroupExistsException() {
        super();
    }

    public GroupExistsException(String message) {
        super(message);
    }

    public GroupExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public GroupExistsException(Throwable cause) {
        super(cause);
    }
}
