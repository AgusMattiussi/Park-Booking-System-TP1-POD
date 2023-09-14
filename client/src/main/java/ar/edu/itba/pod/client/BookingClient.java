package ar.edu.itba.pod.client;

import ar.edu.itba.pod.client.utils.ClientUtils;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rideBooking.RideBookingServiceGrpc;
import rideBooking.RideBookingServiceOuterClass;

import java.security.InvalidParameterException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;


public class BookingClient {

        /*
            $> ./book-cli -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName
            [ -Dday=dayOfYear -Dattraction=rideName -Dvisitor=visitorId -Dslot=bookingSlot
            -DslotTo=bookingSlotTo ]
         */

    private static final Logger logger = LoggerFactory.getLogger(BookingClient.class);
    private static final CountDownLatch latch = new CountDownLatch(1);


    public static void main(String[] args) throws InterruptedException, ExecutionException {
        logger.info("Booking Client Starting ...");

        Map<String, String> argMap = ClientUtils.parseArguments(args);

        //TODO: Validaciones
        //TODO: Validar parametros no null
        final String serverAddress = argMap.get(ClientUtils.SERVER_ADDRESS);
        final String action = argMap.get(ClientUtils.ACTION_NAME);;

        if(serverAddress == null) {
            logger.error("Server address not specified");
            System.exit(1);
        }
        if(action == null) {
            logger.error("Action nos specified");
            System.exit(1);
        }

        ManagedChannel channel = ClientUtils.buildChannel(serverAddress);
        RideBookingServiceGrpc.RideBookingServiceFutureStub stub = RideBookingServiceGrpc.newFutureStub(channel);


        switch (action) {
            case "attractions" -> {

                ListenableFuture<RideBookingServiceOuterClass.GetRideResponse> result =  stub.getRides(Empty.newBuilder().build());
                Futures.addCallback(result, new FutureCallback<>() {
                    @Override
                    public void onSuccess(RideBookingServiceOuterClass.GetRideResponse getRideResponse) {
                        System.out.printf("%-8s- | %-8s- | %-8s- | %-9s- | %-14s%n", "Slot", "Capacity", "Pending", "Confirmed", "Attraction");

                        System.out.printf("%-8s | %-8s | %-8s | %-8s | %-14s%n", "15:30", "30", "0", "30", "SpaceMountain");
                        System.out.printf("%-8s | %-8s | %-8s | %-8s | %-14s%n", "15:45", "30", "3", "27", "SpaceMountain");
                        System.out.printf("%-8s | %-8s | %-8s | %-8s | %-14s%n", "16:00", "30", "0", "5", "SpaceMountain");
                        System.out.printf("Rides: %d\n", getRideResponse.getRidesCount());
                        getRideResponse.getRidesList().forEach(ride -> System.out.printf("%s - From %s to %s\n",
                                ride.getName().getValue(),
                                ride.getOpeningTime().getValue(),
                                ride.getClosingTime().getValue()));

                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        latch.countDown();
                        logger.error(throwable.getMessage());
                    }

                }, Runnable::run);

            }
            case "availability" -> {
                final String rideName = argMap.get(ClientUtils.ATTRACTION);
                final String day = argMap.get(ClientUtils.DAY);
                final String bookingSlot = argMap.get(ClientUtils.BOOKING_SLOT);
                final String bookingSlotTo = argMap.get(ClientUtils.BOOKING_SLOT_TO);

                RideBookingServiceOuterClass.GetRideAvailabilityRequest.Builder builder = RideBookingServiceOuterClass.GetRideAvailabilityRequest.newBuilder()
                        .setDayOfYear(StringValue.of(day))
                        .setStartTimeSlot(StringValue.of(bookingSlot));

                if(bookingSlotTo != null) {
                    builder.setEndTimeSlot(StringValue.of(bookingSlotTo));
                }
                if(rideName != null)
                    builder.setRideName(StringValue.of(rideName));

                ListenableFuture<RideBookingServiceOuterClass.GetRideAvailabilityResponse> result = stub.getRideAvailability(builder.build());

                Futures.addCallback(result, new FutureCallback<>() {
                    @Override
                    public void onSuccess(RideBookingServiceOuterClass.GetRideAvailabilityResponse getRideAvailabilityResponse) {
                        //TODO: Embellecer
                        getRideAvailabilityResponse.getRideAvailabilityList().forEach(rideAvailability -> {
                            String rideName = rideAvailability.getRideName().getValue();
                            rideAvailability.getTimeSlotAvailabilityList().forEach(timeSlotAvailability -> {
                                int capacityVal = timeSlotAvailability.getRideCapacity().getValue();
                                System.out.printf("Slot\t|Capacity\t|Pending\t|Confirmed\t|Attraction\n%s\t|%d\t|%d\t|%d\t|%s",
                                        timeSlotAvailability.getTimeSlot().getValue(),
                                        capacityVal == -1 ? 'X' : capacityVal,
                                        timeSlotAvailability.getPendingBookings().getValue(),
                                        timeSlotAvailability.getConfirmedBookings().getValue(),
                                        rideName
                                        );
                            });
                        });

                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        latch.countDown();
                        logger.error(throwable.getMessage());
                    }
                }, Runnable::run);
            }
            case "book" -> {
                final String rideName = argMap.get(ClientUtils.ATTRACTION);
                final String day = argMap.get(ClientUtils.DAY);
                final String bookingSlot = argMap.get(ClientUtils.BOOKING_SLOT);
                final String visitorId = argMap.get(ClientUtils.VISITOR_ID);

                ListenableFuture<RideBookingServiceOuterClass.BookRideResponse> result = stub.bookRide(
                        RideBookingServiceOuterClass.BookRideRequest.newBuilder()
                                .setRideName(StringValue.of(rideName))
                                .setDayOfYear(StringValue.of(day))
                                .setTimeSlot(StringValue.of(bookingSlot))
                                .setVisitorId(StringValue.of(visitorId))
                                .build());

                Futures.addCallback(result, new FutureCallback<>() {
                    @Override
                    public void onSuccess(RideBookingServiceOuterClass.BookRideResponse bookRideResponse) {
                        System.out.printf("The reservation for %s at %s on the day %s is %s\n",
                                rideName, bookingSlot, day, bookRideResponse.getStatus());
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        latch.countDown();
                        logger.error(throwable.getMessage());
                    }
                }, Runnable::run);
            }
            case "confirm" -> {
                final String rideName = argMap.get(ClientUtils.RIDE_NAME);
                final String day = argMap.get(ClientUtils.DAY);
                final String bookingSlot = argMap.get(ClientUtils.BOOKING_SLOT);
                final String visitorId = argMap.get(ClientUtils.VISITOR_ID);


                ListenableFuture<RideBookingServiceOuterClass.BookRideResponse> result = stub.confirmBooking(
                        RideBookingServiceOuterClass.BookRideRequest.newBuilder()
                                .setRideName(StringValue.of(rideName))
                                .setDayOfYear(StringValue.of(day))
                                .setTimeSlot(StringValue.of(bookingSlot))
                                .setVisitorId(StringValue.of(visitorId))
                                .build());

                Futures.addCallback(result, new FutureCallback<>() {
                    @Override
                    public void onSuccess(RideBookingServiceOuterClass.BookRideResponse bookRideResponse) {
                        System.out.printf("The reservation for %s at %s on the day %s is %s\n",
                                rideName, bookingSlot, day, bookRideResponse.getStatus());
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        latch.countDown();
                        logger.error(throwable.getMessage());
                    }
                }, Runnable::run);
            }
            case "cancel" -> {
                final String rideName = argMap.get(ClientUtils.RIDE_NAME);
                final String day = argMap.get(ClientUtils.DAY);
                final String bookingSlot = argMap.get(ClientUtils.BOOKING_SLOT);
                final String visitorId = argMap.get(ClientUtils.VISITOR_ID);


                ListenableFuture<RideBookingServiceOuterClass.BookRideResponse> result = stub.cancelBooking(
                        RideBookingServiceOuterClass.BookRideRequest.newBuilder()
                                .setRideName(StringValue.of(rideName))
                                .setDayOfYear(StringValue.of(day))
                                .setTimeSlot(StringValue.of(bookingSlot))
                                .setVisitorId(StringValue.of(visitorId))
                                .build());

                Futures.addCallback(result, new FutureCallback<>() {
                    @Override
                    public void onSuccess(RideBookingServiceOuterClass.BookRideResponse bookRideResponse) {
                        System.out.printf("The reservation for %s at %s on the day %s is %s\n",
                                rideName, bookingSlot, day, bookRideResponse.getStatus());
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
            logger.error(e.getMessage());
        }
    }
}