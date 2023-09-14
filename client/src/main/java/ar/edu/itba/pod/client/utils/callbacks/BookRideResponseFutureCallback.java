package ar.edu.itba.pod.client.utils.callbacks;

import org.slf4j.Logger;
import rideBooking.RideBookingServiceOuterClass;

import java.util.concurrent.CountDownLatch;

public class BookRideResponseFutureCallback extends CustomFutureCallback<rideBooking.RideBookingServiceOuterClass.BookRideResponse>{

    private final String rideName;
    private final String bookingSlot;
    private final String day;

    public BookRideResponseFutureCallback(Logger logger, CountDownLatch latch, String rideName, String bookingSlot, String day) {
        super(logger, latch);
        this.rideName = rideName;
        this.bookingSlot = bookingSlot;
        this.day = day;
    }

    @Override
    public void onSuccess(RideBookingServiceOuterClass.BookRideResponse bookRideResponse) {
        System.out.printf("The reservation for %s at %s on the day %s is %s\n",
                rideName, bookingSlot, day, bookRideResponse.getStatus());
        getLatch().countDown();
    }
}
