package ar.edu.itba.pod.grpc.models;

import java.util.Date;

public class ParkPass {
    private final String visitorId;
    private final PassType type;
    private final Date day;

    public ParkPass(String visitorId, PassType type, Date day) {
        this.visitorId = visitorId;
        this.type = type;
        this.day = day;
    }
}
