package ar.edu.itba.pod.server.Models.requests;

import ar.edu.itba.pod.server.Models.ParkLocalTime;

public class AddRideRequestModel {
    private final String rideName;
    private final ParkLocalTime startTime;
    private final ParkLocalTime endTime;
    private final int slotMinutes;

    public AddRideRequestModel(String rideName, ParkLocalTime startTime, ParkLocalTime endTime, int slotMinutes) {
        if(rideName == null)
            throw new IllegalArgumentException("Ride name cannot be null");
        if(startTime == null)
            throw new IllegalArgumentException("Start time cannot be null");
        if(endTime == null)
            throw new IllegalArgumentException("End time cannot be null");
        if(slotMinutes <= 0)
            throw new IllegalArgumentException("Slot minutes must be positive");
        if(startTime.isAfter(endTime))
            throw new IllegalArgumentException("Start time cannot be after end time");

        this.rideName = rideName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotMinutes = slotMinutes;
    }

    public String getRideName() {
        return rideName;
    }

    public ParkLocalTime getStartTime() {
        return startTime;
    }

    public ParkLocalTime getEndTime() {
        return endTime;
    }

    public int getSlotMinutes() {
        return slotMinutes;
    }

    public static AddRideRequestModel fromAddRideRequest(rideBooking.AdminParkServiceOuterClass.AddRideRequest request) {
        return new AddRideRequestModel(request.getRideName(),
                ParkLocalTime.fromString(request.getRideTime().getOpen()),
                ParkLocalTime.fromString(request.getRideTime().getClose()),
                request.getSlotMinutes());
    }
}
