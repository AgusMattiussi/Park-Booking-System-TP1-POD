package ar.edu.itba.pod.server.exceptions;

public class SlotNotFoundException extends RuntimeException{
    public SlotNotFoundException(){
        super();
    }
    public SlotNotFoundException(String message){
        super(message);
    }
}
