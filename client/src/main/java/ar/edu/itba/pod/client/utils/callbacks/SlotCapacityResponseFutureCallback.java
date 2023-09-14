package ar.edu.itba.pod.client.utils.callbacks;

import org.slf4j.Logger;
import rideBooking.AdminParkServiceOuterClass;

import java.util.concurrent.CountDownLatch;

public class SlotCapacityResponseFutureCallback extends CustomFutureCallback<AdminParkServiceOuterClass.SlotCapacityResponse>{

    private final String rideName;
    private final String day;
    private final String capacity;

    public SlotCapacityResponseFutureCallback(Logger logger, CountDownLatch latch, String rideName, String day, String capacity) {
        super(logger, latch);
        this.rideName = rideName;
        this.day = day;
        this.capacity = capacity;
    }

    @Override
    public void onSuccess(AdminParkServiceOuterClass.SlotCapacityResponse slotCapacityResponse) {
        String response = String.format("""
                            Loaded capacity of %s for %s on day %s
                            %s bookings confirmed without changes
                            %s bookings relocated
                            %s bookings cancelled
                            """,
                capacity, rideName, day,
                slotCapacityResponse.getAcceptedAmount(),
                slotCapacityResponse.getRelocatedAmount(),
                slotCapacityResponse.getCancelledAmount());
        System.out.println(response);
        getLatch().countDown();
    }

    @Override
    public void onFailure(Throwable throwable) {
        System.out.printf("Cannot add slot capacity for ride called %s on day %s.%n", rideName, day);
        super.onFailure(throwable);
    }
}
