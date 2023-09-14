package ar.edu.itba.pod.client.utils.callbacks;

import org.slf4j.Logger;
import rideBooking.RideBookingServiceOuterClass;

import java.util.concurrent.CountDownLatch;

public class GetRideAvailabilityResponseFutureCallback extends CustomFutureCallback<RideBookingServiceOuterClass.GetRideAvailabilityResponse>{

    public GetRideAvailabilityResponseFutureCallback(Logger logger, CountDownLatch latch) {
        super(logger, latch);
    }

    public void onSuccess(RideBookingServiceOuterClass.GetRideAvailabilityResponse getRideAvailabilityResponse) {
        System.out.printf("%8s | %-8s | %-7s | %-9s | %-20s |%n", "Slot", "Capacity", "Pending", "Confirmed", "Attraction");

        getRideAvailabilityResponse.getRideAvailabilityList().forEach(rideAvailability -> {
            String rideName = rideAvailability.getRideName().getValue();
            rideAvailability.getTimeSlotAvailabilityList().forEach(timeSlotAvailability -> {
                printRideAvailability(timeSlotAvailability.getTimeSlot().getValue(),
                        timeSlotAvailability.getRideCapacity().getValue(),
                        timeSlotAvailability.getPendingBookings().getValue(),
                        timeSlotAvailability.getConfirmedBookings().getValue(),
                        rideName);
            });
        });

        getLatch().countDown();
    }

    private static void printRideAvailability(String slot, int capacity, int pending, int confirmed, String rideName){
        System.out.printf("%8s | %8s | %7s | %9s | %-20s |%n",
                slot, capacity != -1 ? capacity:"X" , pending, confirmed, rideName);
    }
}
