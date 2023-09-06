package ar.edu.itba.pod.server.exceptions;

public class NotFoundVisitorException extends RuntimeException{
    public NotFoundVisitorException(){
        super();
    }
    public NotFoundVisitorException(String message){
        super(message);
    }
}
