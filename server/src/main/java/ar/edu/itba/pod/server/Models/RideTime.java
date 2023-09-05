package ar.edu.itba.pod.server.Models;

import ar.edu.itba.pod.server.exceptions.InvalidTimeException;
import rideBooking.Models;

import java.time.LocalTime;

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

    public LocalTime  getOpen() {
        return open;
    }

    public LocalTime  getClose() {
        return close;
    }
}
