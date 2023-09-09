package ar.edu.itba.pod.server.client;

import ar.edu.itba.pod.server.client.utils.ClientUtils;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rideBooking.AdminParkServiceGrpc;
import rideBooking.RideBookingServiceGrpc;
import rideBooking.RideBookingServiceOuterClass;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;


public class BookingClient {

        /*
            $> ./book-cli -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName
            [ -Dday=dayOfYear -Dride=rideName -Dvisitor=visitorId -Dslot=bookingSlot
            -DslotTo=bookingSlotTo ]
         */

    private static final Logger logger = LoggerFactory.getLogger(BookingClient.class);
    private static final CountDownLatch latch = new CountDownLatch(1);


    public static void main(String[] args) throws InterruptedException, ExecutionException {
        logger.info("Booking Client Starting ...");

        Map<String, String> argMap = ClientUtils.parseArguments(args);

        //TODO: Validaciones
        final String serverAddress = argMap.get(ClientUtils.SERVER_ADDRESS);
        final String action = argMap.get(ClientUtils.ACTION_NAME);;
        final String day = argMap.get(ClientUtils.DAY);
        final String outPath = argMap.get(ClientUtils.OUTPATH);

        System.out.println("Input parameters:");
        System.out.println("\tServer address: " + serverAddress);
        System.out.println("\tAction: " + action);
        System.out.println("\tDay: " + day);
        System.out.println("\tOut path: " + outPath);
        System.out.println("\n\n");



        ManagedChannel channel = ClientUtils.buildChannel(serverAddress);

        RideBookingServiceGrpc.RideBookingServiceFutureStub stub = RideBookingServiceGrpc.newFutureStub(channel);
        //RideBookingServiceGrpc.RideBookingServiceBlockingStub stub = RideBookingServiceGrpc.newBlockingStub(channel);

        /*final String rideName = argMap.get(ClientUtils.RIDE_NAME);
        final String visitorId = argMap.get(ClientUtils.VISITOR_ID);
        final String bookingSlot = argMap.get(ClientUtils.BOOKING_SLOT);
        final String bookingSlotTo = argMap.get(ClientUtils.BOOKING_SLOT_TO);*/

        switch (action) {
            case "availability" -> {

                ListenableFuture<RideBookingServiceOuterClass.GetRideResponse> result =  stub.getRides(Empty.newBuilder().build());
                Futures.addCallback(result, new FutureCallback<>() {
                    @Override
                    public void onSuccess(RideBookingServiceOuterClass.GetRideResponse getRideResponse) {
                        System.out.println("Success!\n");
                        //TODO: Embellecer
                        System.out.printf("Rides: %d\n", getRideResponse.getRidesCount());
                        getRideResponse.getRidesList().forEach(ride -> System.out.printf("%s - From %s to %s\n",
                                ride.getName().getValue(),
                                ride.getOpeningTime().getValue(),
                                ride.getClosingTime().getValue()));

                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        System.out.println("Error\n");
                        latch.countDown();
                        throw new RuntimeException(throwable);
                    }
                }, Runnable::run);

                //RideBookingServiceOuterClass.GetRideResponse getRideResponse = result.get();



                /*
                RideBookingServiceOuterClass.GetRideResponse getRideResponse = stub.getRides(Empty.newBuilder().build());
                StringBuilder response = new StringBuilder();
                response.append("Rides: \n");
                getRideResponse.getRidesList().forEach(ride -> response.append(String.format("%s - From %s to %s\n",
                        ride.getName().getValue(),
                        ride.getOpeningTime().getValue(),
                        ride.getClosingTime().getValue())));
                System.out.println(response);*/
            }
        }

        try {
            System.out.println("Waiting for response...");
            latch.await(); // Espera hasta que la operación esté completa
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}