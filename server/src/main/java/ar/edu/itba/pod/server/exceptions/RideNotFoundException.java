package ar.edu.itba.pod.server.exceptions;

public class RideNotFoundException extends RuntimeException {
    public RideNotFoundException(){
        super();
    }
    public RideNotFoundException(String message){
        super(message);
    }
}
