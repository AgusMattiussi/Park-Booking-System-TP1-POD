package ar.edu.itba.pod.server.exceptions;

public class InvalidPassTypeException extends RuntimeException{
    public InvalidPassTypeException(){
        super();
    }
    public InvalidPassTypeException(String message){
        super(message);
    }
}
