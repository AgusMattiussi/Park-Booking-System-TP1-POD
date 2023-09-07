package ar.edu.itba.pod.server.Models;

import com.google.protobuf.StringValue;
import rideBooking.QueryServiceOuterClass;

public class ConfirmedBookings implements GRPCModel<rideBooking.QueryServiceOuterClass.ConfirmedBooking>{
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
                .setRideName(StringValue.of(rideName))
                .setVisitorId(StringValue.of(visitorID))
                .setSlot(StringValue.of(slot))
                .build();
    }
}
