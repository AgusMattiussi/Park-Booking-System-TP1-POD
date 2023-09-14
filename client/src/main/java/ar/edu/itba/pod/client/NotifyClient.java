package ar.edu.itba.pod.server.client;

import ar.edu.itba.pod.server.client.utils.ClientUtils;
import com.google.common.util.concurrent.FutureCallback;
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

public class NotifyClient {
    private static final Logger logger = LoggerFactory.getLogger(NotifyClient.class);
    private static final CountDownLatch latch = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException {

        Map<String, String> argMap = ClientUtils.parseArguments(args);

        //TODO: Validaciones
        //TODO: Validar parametros no null
        final String serverAddress = argMap.get(ClientUtils.SERVER_ADDRESS);
        final String action = argMap.get(ClientUtils.ACTION_NAME);;
        final String outPath = argMap.get(ClientUtils.OUTPATH);

        System.out.println("Input parameters:");
        argMap.forEach((key, value) -> {
            if(value != null)
                System.out.printf("%s: %s\n", key, value);
        });
        System.out.println();


        ManagedChannel channel = ClientUtils.buildChannel(serverAddress);
        NotifyServiceGrpc.NotifyServiceFutureStub futureStub = NotifyServiceGrpc.newFutureStub(channel);
        NotifyServiceGrpc.NotifyServiceStub customStub = NotifyServiceGrpc.newStub(channel);


        switch (action) {
            case "follow" -> {

                final String rideName = argMap.get(ClientUtils.RIDE_NAME);
                final String day = argMap.get(ClientUtils.DAY);
                final String visitorID = argMap.get(ClientUtils.VISITOR_ID);

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
                final String rideName = argMap.get(ClientUtils.RIDE_NAME);
                final String day = argMap.get(ClientUtils.DAY);
                final String visitorID = argMap.get(ClientUtils.VISITOR_ID);

                ListenableFuture<NotifyServiceOuterClass.NotificationResponse> result = futureStub.notifyRemoveVisitor(
                        NotifyServiceOuterClass.NotifyRequest.newBuilder()
                                .setRideName(rideName)
                                .setDayOfYear(Integer.parseInt(day))
                                .setVisitorId(visitorID)
                                .build());

                Futures.addCallback(result, new FutureCallback<>() {
                    @Override
                    public void onSuccess(NotifyServiceOuterClass.NotificationResponse notificationResponse) {
                        System.out.println(notificationResponse.getStatus());
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        latch.countDown();
                        logger.error(throwable.getMessage());
                    }
                }, Runnable::run);
            }
            default -> throw new InvalidParameterException(String.format("Action '%s' not supported", action));
        }

        try {
            logger.info("Waiting for response ...");
            latch.await(); // Espera hasta que la operación esté completa
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    private static class NotificationStreamObserver implements StreamObserver<NotifyServiceOuterClass.Notification> {

        @Override
        public void onNext(NotifyServiceOuterClass.Notification notification) {
            //TODO: logger?
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