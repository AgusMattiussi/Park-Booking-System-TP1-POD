package ar.edu.itba.pod.server.exceptions;

public class SlotCapacityException extends RuntimeException{
    public SlotCapacityException(){
        super();
    }
    public SlotCapacityException(String message){
        super(message);
    }
}
