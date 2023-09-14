package ar.edu.itba.pod.server.Models;

import ar.edu.itba.pod.server.Models.interfaces.GRPCModel;
import rideBooking.Models;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

//TODO: Clase privada de Ride?
public class RideTime implements GRPCModel<Models.RideTime> {

    private final ParkLocalTime open;
    private final ParkLocalTime  close;
    private final Duration timeSlotDuration;

    //TODO: Eliminar si no se usa
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

    private ParkLocalTime findFirstValidTimeSlotBetween(ParkLocalTime startTimeSlot, ParkLocalTime endTimeSlot){
        ParkLocalTime current = startTimeSlot;

        if(current.isBefore(open))
            current = open;

        while(current.isBefore(endTimeSlot) && current.isBefore(close) && current.isBefore(startTimeSlot)){
                current = current.plusMinutes(timeSlotDuration.toMinutes());
        }

        return current;
    }

    public List<ParkLocalTime> getTimeSlotsBetween(ParkLocalTime startTimeSlot, ParkLocalTime endTimeSlot){
        if(startTimeSlot.isAfter(endTimeSlot))
            throw new IllegalArgumentException("Start time slot must be before end time slot");

        List<ParkLocalTime> timeSlots = new ArrayList<>();
        ParkLocalTime current = findFirstValidTimeSlotBetween(startTimeSlot, endTimeSlot);

        while(!current.plusMinutes(timeSlotDuration.toMinutes()).isAfter(endTimeSlot) && !current.plusMinutes(timeSlotDuration.toMinutes()).isAfter(close)){
            timeSlots.add(current);
            current = current.plusMinutes(timeSlotDuration.toMinutes());
        }
        return timeSlots;
    }

    public List<String> getTimeSlotsAsStrings(){
        List<String> timeSlots = new ArrayList<>();
        ParkLocalTime current = open;
        while(current.plusMinutes(timeSlotDuration.toMinutes()).isBefore(close)){
            timeSlots.add(current.toString());
            current = current.plusMinutes(timeSlotDuration.toMinutes());
        }
        return timeSlots;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof RideTime))
            return false;

        RideTime other = (RideTime) obj;
        return open.equals(other.open) && close.equals(other.close) && timeSlotDuration.equals(other.timeSlotDuration);
    }
}
