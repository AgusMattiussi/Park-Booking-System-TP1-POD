package ar.edu.itba.pod.server.exceptions;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(){
        super();
    }
    public ReservationNotFoundException(String message){
        super(message);
    }
}
