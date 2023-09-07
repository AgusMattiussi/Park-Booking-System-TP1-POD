package ar.edu.itba.pod.server.exceptions;

public class VisitorNotFoundException extends RuntimeException{
    public VisitorNotFoundException(){
        super();
    }
    public VisitorNotFoundException(String message){
        super(message);
    }
}
