package ar.edu.itba.pod.server.Models;

import rideBooking.Models;

import java.time.Duration;
import java.time.LocalTime;

//TODO: Clase privada de Ride?
public class RideTime implements GRPCModel<Models.RideTime>{

    private final ParkLocalTime open;
    private final ParkLocalTime  close;
    private final Duration timeSlotDuration;

    public RideTime(ParkLocalTime open, ParkLocalTime close, Duration timeSlotDuration){
        this.open = open;
        this.close = close;
        this.timeSlotDuration = timeSlotDuration;
    }

    public RideTime(ParkLocalTime open, ParkLocalTime close, int durationInMinutes){
        this.open = open;
        this.close = close;
        this.timeSlotDuration = Duration.ofMinutes(durationInMinutes);
    }


    public Models.RideTime convertToGRPC(){
        return Models.RideTime.newBuilder()
                .setOpen(open.toString())
                .setClose(close.toString())
                .build();
    }

    public ParkLocalTime getOpen() {
        return open;
    }

    public ParkLocalTime  getClose() {
        return close;
    }

    public Duration getTimeSlotDuration() {
        return timeSlotDuration;
    }

    public boolean isTimeSlotValid(ParkLocalTime timeSlot){
        if(timeSlot.isBefore(open) || timeSlot.isAfter(close)){
            return false;
        }

        long openedMinutesToTimeSlot = Duration.between(open.asLocalTime(), timeSlot.asLocalTime()).toMinutes();
        return openedMinutesToTimeSlot % timeSlotDuration.toMinutes() == 0;
    }

    public boolean isTimeSlotValid(LocalTime timeSlot){
        return isTimeSlotValid(new ParkLocalTime(timeSlot));
    }

    public boolean isTimeSlotValid(String timeSlot){
        return isTimeSlotValid(ParkLocalTime.fromString(timeSlot));
    }

}
