package ch.semafor.gendas.exceptions;

public class ElementTypeCreationException extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ElementTypeCreationException(final String msg) {
        super(msg);
    }

    public ElementTypeCreationException(final String msg, Throwable e) {
        super(msg, e);
    }

}
