package ar.edu.itba.pod.server.Models.requests;

import rideBooking.NotifyServiceOuterClass;

import java.util.UUID;

public class NotifyRequestModel {

    private final String rideName;
    private final UUID visitorId;
    private final int day;

    public NotifyRequestModel(String rideName, UUID visitorId, int day) {
        if(day < 1 || day > 365)
            throw new IllegalArgumentException("Day must be between 1 and 365");

        if(rideName == null)
            throw new IllegalArgumentException("Ride name must not be null");

        if (visitorId == null)
            throw new IllegalArgumentException("Visitor ID must not be null");

        this.rideName = rideName;
        this.visitorId = visitorId;
        this.day = day;
    }

    public String getRideName() {
        return rideName;
    }

    public UUID getVisitorId() {
        return visitorId;
    }

    public int getDay() {
        return day;
    }

    public static NotifyRequestModel fromNotifyRequest(NotifyServiceOuterClass.NotifyRequest request){
        return new NotifyRequestModel(request.getRideName(), UUID.fromString(request.getVisitorId()), request.getDayOfYear());
    }
}
