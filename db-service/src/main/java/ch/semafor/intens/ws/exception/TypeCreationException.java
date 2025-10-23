package ch.semafor.intens.ws.exception;

public class TypeCreationException extends RuntimeException {
    public TypeCreationException(String msg, Throwable throwable){
        super(msg, throwable);
    }
}
