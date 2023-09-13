package ar.edu.itba.pod.server.exceptions;

public class ReservationLimitException extends RuntimeException{
    public ReservationLimitException(String message){
        super(message);
    }
}
