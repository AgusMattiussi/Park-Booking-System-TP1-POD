package ar.edu.itba.pod.client;

import ar.edu.itba.pod.client.utils.ClientUtils;
import ar.edu.itba.pod.client.utils.callbacks.NotificationResponseFutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rideBooking.NotifyServiceGrpc;
import rideBooking.NotifyServiceOuterClass;

import java.security.InvalidParameterException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static ar.edu.itba.pod.client.utils.ClientUtils.validateNullArgument;

public class NotifyClient {
    private static final Logger logger = LoggerFactory.getLogger(NotifyClient.class);
    private static final CountDownLatch latch = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException {
        logger.info("Notify Client Starting...");
        Map<String, String> argMap = ClientUtils.parseArguments(args);

        final String serverAddress = argMap.get(ClientUtils.SERVER_ADDRESS);
        final String action = argMap.get(ClientUtils.ACTION_NAME);
        final String rideName = argMap.get(ClientUtils.RIDE_NAME);
        final String day = argMap.get(ClientUtils.DAY);
        final String visitorID = argMap.get(ClientUtils.VISITOR_ID);

        validateNullArgument(serverAddress, "Server address not specified");
        validateNullArgument(action, "Action not specified");
        validateNullArgument(rideName, "Ride name not specified");
        validateNullArgument(day, "Day not specified");
        validateNullArgument(visitorID, "Visitor ID not specified");

        ManagedChannel channel = ClientUtils.buildChannel(serverAddress);

        NotifyServiceGrpc.NotifyServiceFutureStub futureStub = NotifyServiceGrpc.newFutureStub(channel);
        NotifyServiceGrpc.NotifyServiceStub customStub = NotifyServiceGrpc.newStub(channel);


        switch (action) {
            case "follow" -> {

                NotifyServiceOuterClass.NotifyRequest request = NotifyServiceOuterClass.NotifyRequest.newBuilder()
                        .setRideName(rideName)
                        .setDayOfYear(Integer.parseInt(day))
                        .setVisitorId(visitorID)
                        .build();

                StreamObserver<NotifyServiceOuterClass.Notification> observer = new NotificationStreamObserver();

                customStub.notifyVisitor(request, observer);
                latch.await();

            }
            case "unfollow" -> {

                ListenableFuture<NotifyServiceOuterClass.NotificationResponse> result = futureStub.notifyRemoveVisitor(
                        NotifyServiceOuterClass.NotifyRequest.newBuilder()
                                .setRideName(rideName)
                                .setDayOfYear(Integer.parseInt(day))
                                .setVisitorId(visitorID)
                                .build());

                Futures.addCallback(result, new NotificationResponseFutureCallback(logger, latch, rideName), Runnable::run);
            }
            default -> throw new InvalidParameterException(String.format("Action '%s' not supported", action));
        }

        try {
            logger.info("Waiting for response...");
            latch.await(); // Espera hasta que la operación esté completa
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    private static class NotificationStreamObserver implements StreamObserver<NotifyServiceOuterClass.Notification> {

        @Override
        public void onNext(NotifyServiceOuterClass.Notification notification) {
            System.out.println(notification.getMessage());
        }

        @Override
        public void onError(Throwable throwable) {
            latch.countDown();
            logger.error(throwable.getMessage());
        }

        @Override
        public void onCompleted() {
            latch.countDown();
            logger.info("Completed");
        }
    }
}