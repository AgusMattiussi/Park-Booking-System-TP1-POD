package ar.edu.itba.pod.server.Models;

import ar.edu.itba.pod.server.exceptions.InvalidTimeException;
import rideBooking.Models;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class RideTime implements GRPCModel<Models.RideTime>{

    private final LocalTime open;
    private final LocalTime  close;

    public RideTime(LocalTime open, LocalTime  close){
        this.open = open;
        this.close = close;
    }

    public RideTime(Models.RideTime grpcRideTime){
        try{
            this.open = LocalTime.parse(grpcRideTime.getOpen());
            this.close = LocalTime.parse(grpcRideTime.getClose());
        }catch (Exception e){
            throw new InvalidTimeException("Open or Close time is invalid");
        }

    }

    public Models.RideTime convertToGRPC(){
        return Models.RideTime.newBuilder()
                .setOpen(String.valueOf(this.open))
                .setClose(String.valueOf(this.close))
                .build();
    }

    public LocalTime getOpen() {
        return open;
    }

    /* Returns a LocalTime in a HH:mm formatted String */
    private String localTimeToFormattedString(LocalTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return time.format(formatter);
    }

    /* Returns Open Time in HH:mm format */
    public String getOpenTimeFormattedString() {
        return localTimeToFormattedString(this.open);
    }

    public LocalTime  getClose() {
        return close;
    }

    /* Returns Close Time in HH:mm format */
    public String getCloseTimeFormattedString() {
        return localTimeToFormattedString(this.close);
    }
}
