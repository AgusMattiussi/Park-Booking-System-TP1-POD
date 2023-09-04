package ar.edu.itba.pod.server.Models;


import rideBooking.Models.PassTypeEnum;

import java.util.Date;

public class ParkPass {
    private final String visitorId;
    private final PassTypeEnum type;
    private final Date day;

    public ParkPass(String visitorId, PassTypeEnum type, Date day) {
        this.visitorId = visitorId;
        this.type = type;
        this.day = day;
    }
}
