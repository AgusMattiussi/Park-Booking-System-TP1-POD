package ar.edu.itba.pod.server.exceptions;

public class NotFoundSlotException extends RuntimeException{
    public NotFoundSlotException(){
        super();
    }
    public NotFoundSlotException(String message){
        super(message);
    }
}
