package ar.edu.itba.pod.server.exceptions;

public class AlreadyConfirmedException extends RuntimeException{
    public AlreadyConfirmedException(){
        super();
    }
    public AlreadyConfirmedException(String message){
        super(message);
    }
}
