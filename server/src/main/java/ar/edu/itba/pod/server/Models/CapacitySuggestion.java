package ar.edu.itba.pod.server.Models;

import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import rideBooking.QueryServiceOuterClass;

public class CapacitySuggestion implements GRPCModel<QueryServiceOuterClass.CapacitySuggestion> {
    private final String rideName;
    private final int suggestedCapacity;
    private final String slot;

    public CapacitySuggestion(String rideName, int suggestedCapacity, String slot) {
        this.rideName = rideName;
        this.suggestedCapacity = suggestedCapacity;
        this.slot = slot;
    }

    public String getRideName() {
        return rideName;
    }

    public int getSuggestedCapacity() {
        return suggestedCapacity;
    }

    public String getSlot() {
        return slot;
    }

    @Override
    public QueryServiceOuterClass.CapacitySuggestion convertToGRPC() {
        return QueryServiceOuterClass.CapacitySuggestion.newBuilder()
                .setRideName(StringValue.of(rideName))
                .setSuggestedCapacity(Int32Value.of(suggestedCapacity))
                .setSlot(StringValue.of(slot))
                .build();
    }
}
