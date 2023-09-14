package ar.edu.itba.pod.client.utils.callbacks;

import org.slf4j.Logger;
import rideBooking.RideBookingServiceOuterClass;

import java.util.concurrent.CountDownLatch;

public class GetRideResponseFutureCallback extends CustomFutureCallback<RideBookingServiceOuterClass.GetRideResponse> {

    public GetRideResponseFutureCallback(Logger logger, CountDownLatch latch) {
        super(logger, latch);
    }

    @Override
    public void onSuccess(RideBookingServiceOuterClass.GetRideResponse getRideResponse) {
        System.out.printf(" %-20s | %9s | %10s |%n", "Attraction", "Open Time", "Close Time");

        getRideResponse.getRidesList().forEach(ride -> System.out.printf(" %-20s | %9s | %10s |%n",
            ride.getName().getValue(),
            ride.getOpeningTime().getValue(),
            ride.getClosingTime().getValue()));

        getLatch().countDown();
    }
}
