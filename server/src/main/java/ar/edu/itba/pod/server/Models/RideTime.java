package ar.edu.itba.pod.server.Models;

import rideBooking.Models;

public class RideTime implements GRPCModel<Models.RideTime>{

    private Double open;
    private Double close;

    public RideTime(Double open, Double close){
        this.open = open;
        this.close = close;
    }

    public RideTime(Models.RideTime grpcRideTime){
        this.open = grpcRideTime.getOpen();
        this.close = grpcRideTime.getClose();
    }

    public Models.RideTime convertToGRPC(){
        return Models.RideTime.newBuilder()
                .setOpen(this.open)
                .setClose(this.close)
                .build();
    }


}
