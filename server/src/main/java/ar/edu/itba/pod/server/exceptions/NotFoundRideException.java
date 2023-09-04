package ar.edu.itba.pod.server.exceptions;

public class NotFoundRideException extends RuntimeException{
    public NotFoundRideException(){
        super();
    }
    public NotFoundRideException(String message){
        super(message);
    }
}
