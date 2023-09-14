package ar.edu.itba.pod.client.utils.callbacks;

import org.slf4j.Logger;
import rideBooking.NotifyServiceOuterClass;

import java.util.concurrent.CountDownLatch;

public class NotificationResponseFutureCallback extends CustomFutureCallback<NotifyServiceOuterClass.NotificationResponse> {

    private final String rideName;

    public NotificationResponseFutureCallback(Logger logger, CountDownLatch latch, String rideName) {
        super(logger, latch);
        this.rideName = rideName;
    }

    @Override
    public void onSuccess(NotifyServiceOuterClass.NotificationResponse notificationResponse) {
        System.out.printf("Response Status: %s - Successfully unsubscribed from %s ride notifications\n",
                notificationResponse.getStatus(), rideName);
        getLatch().countDown();
    }

}
