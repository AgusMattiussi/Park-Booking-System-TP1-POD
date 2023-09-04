package ar.edu.itba.pod.server.Models;
import rideBooking.Models.PassTypeEnum;

import java.util.UUID;

public class ParkPass {
    private final UUID visitorId;
    private final PassTypeEnum type;
    private final int day;

    public ParkPass(UUID visitorId, PassTypeEnum type, int day) {
        this.visitorId = visitorId;
        this.type = type;
        this.day = day;
    }

    public int getDay() {
        return day;
    }

    public PassTypeEnum getType() {
        return type;
    }

    public UUID getVisitorId() {
        return visitorId;
    }
}
