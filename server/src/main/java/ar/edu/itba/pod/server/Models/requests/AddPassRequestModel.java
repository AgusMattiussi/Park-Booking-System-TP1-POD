package ar.edu.itba.pod.server.Models.requests;

import rideBooking.Models;

import java.util.UUID;

public class AddPassRequestModel {
    private final UUID visitorId;
    private final Models.PassTypeEnum passType;
    private final int day;

    public AddPassRequestModel(UUID visitorId, Models.PassTypeEnum passType, int day) {
        if(visitorId == null)
            throw new IllegalArgumentException("Visitor id cannot be null");
        if(passType == null)
            throw new IllegalArgumentException("Pass type cannot be null");
        if(day <= 0 || day > 365)
            throw new IllegalArgumentException("Day must be between 1 and 365");

        this.visitorId = visitorId;
        this.passType = passType;
        this.day = day;
    }

    public UUID getVisitorId() {
        return visitorId;
    }

    public Models.PassTypeEnum getPassType() {
        return passType;
    }

    public int getDay() {
        return day;
    }

    public static AddPassRequestModel fromAddPassRequest(rideBooking.AdminParkServiceOuterClass.AddPassRequest request) {
        return new AddPassRequestModel(
                UUID.fromString(request.getVisitorId()),
                request.getPassType(),
                request.getValidDay());
    }


}
