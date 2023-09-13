package ar.edu.itba.pod.server.Models;

import rideBooking.Models;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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
        if(open.isAfter(close))
            throw new IllegalArgumentException("Open time must be before close time");
        if(durationInMinutes <= 0)
            throw new IllegalArgumentException("Duration must be positive");

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

    public List<String> getTimeSlotsAsStrings(){
        List<String> timeSlots = new ArrayList<>();
        ParkLocalTime current = open;
        while(current.isBefore(close)){
            timeSlots.add(current.toString());
            current = current.plusMinutes(timeSlotDuration.toMinutes());
        }
        return timeSlots;
    }

}
