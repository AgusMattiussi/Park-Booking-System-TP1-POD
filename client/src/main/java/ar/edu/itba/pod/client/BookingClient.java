package ar.edu.itba.pod.client;

import ar.edu.itba.pod.client.utils.ClientUtils;
import ar.edu.itba.pod.client.utils.callbacks.BookRideResponseFutureCallback;
import ar.edu.itba.pod.client.utils.callbacks.GetRideAvailabilityResponseFutureCallback;
import ar.edu.itba.pod.client.utils.callbacks.GetRideResponseFutureCallback;
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

import static ar.edu.itba.pod.client.utils.ClientUtils.validateNullArgument;


public class BookingClient {

    private static final Logger logger = LoggerFactory.getLogger(BookingClient.class);
    private static final CountDownLatch latch = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        logger.info("Booking Client Starting...");

        Map<String, String> argMap = ClientUtils.parseArguments(args);

        final String serverAddress = argMap.get(ClientUtils.SERVER_ADDRESS);
        final String action = argMap.get(ClientUtils.ACTION_NAME);

        validateNullArgument(serverAddress, "Ride name not specified");
        validateNullArgument(action, "Action not specified");

        ManagedChannel channel = ClientUtils.buildChannel(serverAddress);
        RideBookingServiceGrpc.RideBookingServiceFutureStub stub = RideBookingServiceGrpc.newFutureStub(channel);

        switch (action) {
            case "attractions" -> {
                ListenableFuture<RideBookingServiceOuterClass.GetRideResponse> result =  stub.getRides(Empty.newBuilder().build());
                Futures.addCallback(result, new GetRideResponseFutureCallback(logger, latch), Runnable::run);
            }
            case "availability" -> {
                final String rideName = argMap.get(ClientUtils.ATTRACTION);
                final String day = argMap.get(ClientUtils.DAY);
                final String bookingSlot = argMap.get(ClientUtils.BOOKING_SLOT);
                final String bookingSlotTo = argMap.get(ClientUtils.BOOKING_SLOT_TO);

                validateNullArgument(rideName, "Attraction name not specified");
                validateNullArgument(day, "Day not specified");
                validateNullArgument(bookingSlot, "Booking slot not specified");
                validateNullArgument(bookingSlot, "Booking slot to not specified");

                RideBookingServiceOuterClass.GetRideAvailabilityRequest.Builder builder = RideBookingServiceOuterClass.GetRideAvailabilityRequest.newBuilder()
                        .setDayOfYear(StringValue.of(day))
                        .setStartTimeSlot(StringValue.of(bookingSlot));

                if(bookingSlotTo != null) {
                    builder.setEndTimeSlot(StringValue.of(bookingSlotTo));
                }
                if(rideName != null)
                    builder.setRideName(StringValue.of(rideName));

                ListenableFuture<RideBookingServiceOuterClass.GetRideAvailabilityResponse> result = stub.getRideAvailability(builder.build());
                Futures.addCallback(result, new GetRideAvailabilityResponseFutureCallback(logger, latch), Runnable::run);
            }
            case "book" -> {
                final String rideName = argMap.get(ClientUtils.ATTRACTION);
                final String day = argMap.get(ClientUtils.DAY);
                final String bookingSlot = argMap.get(ClientUtils.BOOKING_SLOT);
                final String visitorId = argMap.get(ClientUtils.VISITOR_ID);

                validateNullArgument(rideName, "Attraction name not specified");
                validateNullArgument(day, "Day not specified");
                validateNullArgument(bookingSlot, "Booking slot not specified");
                validateNullArgument(visitorId, "Visitor ID not specified");

                ListenableFuture<RideBookingServiceOuterClass.BookRideResponse> result = stub.bookRide(
                        RideBookingServiceOuterClass.BookRideRequest.newBuilder()
                                .setRideName(StringValue.of(rideName))
                                .setDayOfYear(StringValue.of(day))
                                .setTimeSlot(StringValue.of(bookingSlot))
                                .setVisitorId(StringValue.of(visitorId))
                                .build());

                Futures.addCallback(result, new BookRideResponseFutureCallback(logger, latch, rideName, bookingSlot, day),
                        Runnable::run);
            }
            case "confirm" -> {
                final String rideName = argMap.get(ClientUtils.ATTRACTION);
                final String day = argMap.get(ClientUtils.DAY);
                final String bookingSlot = argMap.get(ClientUtils.BOOKING_SLOT);
                final String visitorId = argMap.get(ClientUtils.VISITOR_ID);

                validateNullArgument(rideName, "Attraction name not specified");
                validateNullArgument(day, "Day not specified");
                validateNullArgument(bookingSlot, "Booking slot not specified");
                validateNullArgument(visitorId, "Visitor ID not specified");

                ListenableFuture<RideBookingServiceOuterClass.BookRideResponse> result = stub.confirmBooking(
                        RideBookingServiceOuterClass.BookRideRequest.newBuilder()
                                .setRideName(StringValue.of(rideName))
                                .setDayOfYear(StringValue.of(day))
                                .setTimeSlot(StringValue.of(bookingSlot))
                                .setVisitorId(StringValue.of(visitorId))
                                .build());

                Futures.addCallback(result, new BookRideResponseFutureCallback(logger, latch, rideName, bookingSlot, day),
                        Runnable::run);
            }
            case "cancel" -> {
                final String rideName = argMap.get(ClientUtils.ATTRACTION);
                final String day = argMap.get(ClientUtils.DAY);
                final String bookingSlot = argMap.get(ClientUtils.BOOKING_SLOT);
                final String visitorId = argMap.get(ClientUtils.VISITOR_ID);

                validateNullArgument(rideName, "Attraction name not specified");
                validateNullArgument(day, "Day not specified");
                validateNullArgument(bookingSlot, "Booking slot not specified");
                validateNullArgument(visitorId, "Visitor ID not specified");

                ListenableFuture<RideBookingServiceOuterClass.BookRideResponse> result = stub.cancelBooking(
                        RideBookingServiceOuterClass.BookRideRequest.newBuilder()
                                .setRideName(StringValue.of(rideName))
                                .setDayOfYear(StringValue.of(day))
                                .setTimeSlot(StringValue.of(bookingSlot))
                                .setVisitorId(StringValue.of(visitorId))
                                .build());

                Futures.addCallback(result, new BookRideResponseFutureCallback(logger, latch, rideName, bookingSlot, day),
                        Runnable::run);
            }
            default -> throw new InvalidParameterException(String.format("Action '%s' not supported", action));
        }

        try {
            logger.info("Waiting for response...");
            latch.await(); // Espera hasta que la operación esté completa
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(e.getMessage());
        }
    }
}