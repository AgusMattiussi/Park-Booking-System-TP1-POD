package ar.edu.itba.pod.server.Models.requests;

import ar.edu.itba.pod.server.Models.ParkLocalTime;
import rideBooking.RideBookingServiceOuterClass;

public class GetRideAvailabilityRequestModel {
    int day;
    String rideName;
    ParkLocalTime startTimeSlot;
    ParkLocalTime endTimeSlot;

    public GetRideAvailabilityRequestModel(int day, String rideName, ParkLocalTime startTimeSlot, ParkLocalTime endTimeSlot) {
        if(day < 1 || day > 365)
            throw new IllegalArgumentException("Day must be between 1 and 365");

        this.day = day;
        this.rideName = rideName;
        this.startTimeSlot = startTimeSlot;
        this.endTimeSlot = endTimeSlot;
    }

    public int getDay() {
        return day;
    }

    public String getRideName() {
        return rideName;
    }

    public ParkLocalTime getStartTimeSlot() {
        return startTimeSlot;
    }

    public ParkLocalTime getEndTimeSlot() {
        return endTimeSlot;
    }

    public static GetRideAvailabilityRequestModel fromGetRideAvailabilityRequest(RideBookingServiceOuterClass.GetRideAvailabilityRequest request){
        ParkLocalTime startTimeSlot = ParkLocalTime.fromString(request.getStartTimeSlot().getValue());
        ParkLocalTime endTimeSlot = null;
        String rideName = null;
        int dayOfTheYear = Integer.parseInt(request.getDayOfYear().getValue());

        if(request.hasEndTimeSlot()) {
           endTimeSlot = ParkLocalTime.fromString(request.getEndTimeSlot().getValue());
        }

        if(request.hasRideName())
            rideName = request.getRideName().getValue();

        return new GetRideAvailabilityRequestModel(dayOfTheYear, rideName, startTimeSlot, endTimeSlot);
    }
}
