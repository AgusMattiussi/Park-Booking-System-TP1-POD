package ar.edu.itba.pod.server.Models;

import ar.edu.itba.pod.server.Models.interfaces.GRPCModel;
import rideBooking.QueryServiceOuterClass;

public class ConfirmedBookings implements GRPCModel<QueryServiceOuterClass.ConfirmedBooking> {
    private final String rideName;
    private final String visitorID;
    private final String slot;

    public ConfirmedBookings(String rideName, String visitorID, String slot) {
        this.rideName = rideName;
        this.visitorID = visitorID;
        this.slot = slot;
    }

    public String getRideName() {
        return rideName;
    }

    public String getVisitorID() {
        return visitorID;
    }

    public String getSlot() {
        return slot;
    }

    @Override
    public QueryServiceOuterClass.ConfirmedBooking convertToGRPC() {
        return QueryServiceOuterClass.ConfirmedBooking.newBuilder()
                .setRideName(rideName)
                .setVisitorId(visitorID)
                .setSlot(slot)
                .build();
    }
}
