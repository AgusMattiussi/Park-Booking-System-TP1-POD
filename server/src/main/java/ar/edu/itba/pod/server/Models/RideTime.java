package ar.edu.itba.pod.server.Models;

import rideBooking.Models;

public class RideTime implements GRPCModel<Models.RideTime>{

    private final ParkLocalTime open;
    private final ParkLocalTime  close;

    public RideTime(ParkLocalTime open, ParkLocalTime close){
        this.open = open;
        this.close = close;
    }

    public RideTime(Models.RideTime grpcRideTime){
        this.open = ParkLocalTime.fromString(grpcRideTime.getOpen());
        this.close = ParkLocalTime.fromString(grpcRideTime.getClose());
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

}
