package ar.edu.itba.pod.server.exceptions;

public class NotFoundPassException extends RuntimeException{
    public NotFoundPassException(){
        super();
    }
    public NotFoundPassException(String message){
        super(message);
    }
}
