package ar.edu.itba.pod.server.Models.requests;

import rideBooking.AdminParkServiceOuterClass;

public class AddSlotCapacityRequestModel {

    private final String rideName;
    private final int day;
    private final int slotCapacity;

    public AddSlotCapacityRequestModel(String rideName, int day, int slotCapacity) {
        if(rideName == null)
            throw new IllegalArgumentException("Ride name cannot be null");
        if(day <= 0 || day > 365)
            throw new IllegalArgumentException("Day must be between 1 and 365");
        if(slotCapacity <= 0)
            throw new IllegalArgumentException("Slot capacity must be greater than 0");

        this.rideName = rideName;
        this.day = day;
        this.slotCapacity = slotCapacity;
    }

    public String getRideName() {
        return rideName;
    }

    public int getDay() {
        return day;
    }

    public int getSlotCapacity() {
        return slotCapacity;
    }

    public static AddSlotCapacityRequestModel fromAddSlotCapacityRequest(AdminParkServiceOuterClass.AddSlotCapacityRequest request){
        return new AddSlotCapacityRequestModel(request.getRideName(), request.getValidDay(), request.getSlotCapacity());
    }
}
