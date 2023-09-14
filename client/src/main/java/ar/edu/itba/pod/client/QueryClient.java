package ar.edu.itba.pod.client;

import ar.edu.itba.pod.client.utils.ClientUtils;
import ar.edu.itba.pod.client.utils.callbacks.CapacitySuggestionResponseFutureCallback;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rideBooking.QueryServiceGrpc;
import rideBooking.QueryServiceOuterClass;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static ar.edu.itba.pod.client.utils.ClientUtils.*;

public class QueryClient {
    private static final Logger logger = LoggerFactory.getLogger(QueryClient.class);
    private static final CountDownLatch latch = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException {
        logger.info("Query Client Starting...");

        Map<String, String> argMap = parseArguments(args);

        final String serverAddress = getArgumentValue(argMap, ClientUtils.SERVER_ADDRESS);
        final String action = getArgumentValue(argMap, ClientUtils.ACTION_NAME);;
        final String day = getArgumentValue(argMap, ClientUtils.DAY);
        final String outPath = getArgumentValue(argMap, ClientUtils.OUTPATH);

        validateNullArgument(serverAddress, "Server address not specified");
        validateNullArgument(action, "Action not specified");
        validateNullArgument(day, "Day not specified");
        validateNullArgument(outPath, "Output path not specified");

        ManagedChannel channel = buildChannel(serverAddress);

        QueryServiceGrpc.QueryServiceFutureStub stub = QueryServiceGrpc.newFutureStub(channel);

        switch(action){
            case "capacity" -> {
                logger.info("Capacity Suggestion Query");

                ListenableFuture<QueryServiceOuterClass.CapacitySuggestionResponse> result = stub.queryCapacitySuggestion(
                        QueryServiceOuterClass.QueryDayRequest.newBuilder().setDayOfYear(day).build()
                );

                Futures.addCallback(result, new CapacitySuggestionResponseFutureCallback(logger, latch, outPath),
                        Runnable::run);
            }
            case "confirmed" -> {
                logger.info("Confirmed Bookings Query");

                ListenableFuture<QueryServiceOuterClass.ConfirmedBookingsResponse> result = stub.queryConfirmedBookings(
                        QueryServiceOuterClass.QueryDayRequest.newBuilder().setDayOfYear(day).build()
                );

                Futures.addCallback(result, new FutureCallback<>() {
                    @Override
                    public void onSuccess(QueryServiceOuterClass.ConfirmedBookingsResponse confirmedBookingsResponse) {
                        System.out.printf("Successfully generated output file %s\n", outPath);
                        List<QueryServiceOuterClass.ConfirmedBooking> list = confirmedBookingsResponse.getConfirmedBookingsList();
                        generateConfirmedQueryFileContent(list, outPath);
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        latch.countDown();
                        logger.error(throwable.getMessage());
                    }}, Runnable::run);
            }
            default -> logger.error("Invalid action");
        }

        try {
            logger.info("Waiting for response...");
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    private static void generateConfirmedQueryFileContent(List<QueryServiceOuterClass.ConfirmedBooking> list, String outPath){
        StringBuilder sb = new StringBuilder();
        sb.append("Slot\t| Visitor\t\t\t\t\t\t\t\t| Ride\n");
        for(QueryServiceOuterClass.ConfirmedBooking confirmedBooking : list){
            sb.append(confirmedBooking.getSlot()).append("\t| ")
                    .append(confirmedBooking.getVisitorId()).append("  | ")
                    .append(confirmedBooking.getRideName()).append("\n");
        }
        createOutputFile(outPath, sb.toString());
    }
}