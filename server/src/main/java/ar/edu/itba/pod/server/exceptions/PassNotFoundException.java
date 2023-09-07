package ar.edu.itba.pod.server.exceptions;

public class PassNotFoundException extends RuntimeException{
    public PassNotFoundException(){
        super();
    }
    public PassNotFoundException(String message){
        super(message);
    }
}
