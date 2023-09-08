package ar.edu.itba.pod.server.Models.requests;


/*message BookRideRequest {
  google.protobuf.StringValue ride_name = 1;
  google.protobuf.StringValue day_of_year = 2;
  google.protobuf.StringValue time_slot = 3;
  google.protobuf.StringValue visitor_id = 4;
}*/

import ar.edu.itba.pod.server.Models.ParkLocalTime;
import rideBooking.RideBookingServiceOuterClass;

import java.util.UUID;

public class BookRideRequestModel {
    String rideName;
    int day;
    ParkLocalTime timeSlot;
    UUID visitorId;

    public BookRideRequestModel(String rideName, int day, ParkLocalTime timeSlot, UUID visitorId) {
        if(day < 1 || day > 365)
            throw new IllegalArgumentException("Day must be between 1 and 365");

        if(timeSlot == null)
            throw new IllegalArgumentException("Time slot must not be null");

        if(rideName == null)
            throw new IllegalArgumentException("Ride name must not be null");

        if(visitorId == null)
            throw new IllegalArgumentException("Visitor ID must not be null");

        this.rideName = rideName;
        this.day = day;
        this.timeSlot = timeSlot;
        this.visitorId = visitorId;
    }

    public String getRideName() {
        return rideName;
    }

    public int getDay() {
        return day;
    }

    public ParkLocalTime getTimeSlot() {
        return timeSlot;
    }

    public UUID getVisitorId() {
        return visitorId;
    }

    public static BookRideRequestModel fromBookRideRequest(RideBookingServiceOuterClass.BookRideRequest request){
        if(!request.hasDayOfYear())
            throw new IllegalArgumentException("Must provide a day");

        if(!request.hasVisitorId())
            throw new IllegalArgumentException("Must provide a visitor ID");

        if(!request.hasTimeSlot())
            throw new IllegalArgumentException("Must provide a time slot");

        String rideName = request.getRideName().getValue();
        int dayOfTheYear = Integer.parseInt(request.getDayOfYear().getValue());
        ParkLocalTime timeSlot = ParkLocalTime.fromString(request.getTimeSlot().getValue());
        UUID visitorId = UUID.fromString(request.getVisitorId().getValue());

        return new BookRideRequestModel(rideName, dayOfTheYear, timeSlot, visitorId);
    }
}
