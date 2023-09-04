package ar.edu.itba.pod.server.exceptions;

public class InvalidTimeException extends RuntimeException{
    public InvalidTimeException(){
        super();
    }
    public InvalidTimeException(String message){
        super(message);
    }
}
