package ar.edu.itba.pod.server.exceptions;

public class AlreadyExistsException extends RuntimeException{
    public AlreadyExistsException(){
        super();
    }
    public AlreadyExistsException(String message){
        super(message);
    }
}
