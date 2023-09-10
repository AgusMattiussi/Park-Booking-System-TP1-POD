package ar.edu.itba.pod.server.client;

import ar.edu.itba.pod.server.client.utils.ClientUtils;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.StringValue;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rideBooking.QueryServiceGrpc;
import rideBooking.QueryServiceOuterClass;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static ar.edu.itba.pod.server.client.utils.ClientUtils.*;

public class QueryClient {
    private static final Logger logger = LoggerFactory.getLogger(QueryClient.class);
    private static final CountDownLatch latch = new CountDownLatch(1);

    //     ./query-cli -DserverAddress=10.6.0.1:50051 -Daction=capacity/confirmed -Dday=100 -DoutPath=query1.txt
    public static void main(String[] args) throws InterruptedException {
        logger.info("Query Client Starting ...");

        Map<String, String> argMap = parseArguments(args);

        final String serverAddress = getArgumentValue(argMap, ClientUtils.SERVER_ADDRESS);
        final String action = getArgumentValue(argMap, ClientUtils.ACTION_NAME);;
        final String day = getArgumentValue(argMap, ClientUtils.DAY);
        final String outPath = getArgumentValue(argMap, ClientUtils.OUTPATH);

        ManagedChannel channel = buildChannel(serverAddress);

        //TODO: blockingStub?
        QueryServiceGrpc.QueryServiceFutureStub stub = QueryServiceGrpc.newFutureStub(channel);

        switch(action){
            case "capacity" -> {
                logger.info("Capacity Suggestion Query\n");

                ListenableFuture<QueryServiceOuterClass.CapacitySuggestionResponse> result = stub.queryCapacitySuggestion(
                        QueryServiceOuterClass.QueryDayRequest.newBuilder().setDayOfYear(StringValue.of(day)).build()
                );

                Futures.addCallback(result, new FutureCallback<>() {
                    @Override
                    public void onSuccess(QueryServiceOuterClass.CapacitySuggestionResponse capacitySuggestionResponse) {
                        List<QueryServiceOuterClass.CapacitySuggestion> list = capacitySuggestionResponse.getCapacitySuggestionsList();
                        generateCapacityQueryFileContent(list, outPath);
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        System.out.println("Error\n");
                        latch.countDown();
                        System.err.println(throwable.getMessage());
                    }}, Runnable::run);
            }
            case "confirmed" -> {
                logger.info("Confirmed Bookings Query\n");

                ListenableFuture<QueryServiceOuterClass.ConfirmedBookingsResponse> result = stub.queryConfirmedBookings(
                        QueryServiceOuterClass.QueryDayRequest.newBuilder().setDayOfYear(StringValue.of(day)).build()
                );

                Futures.addCallback(result, new FutureCallback<>() {
                    @Override
                    public void onSuccess(QueryServiceOuterClass.ConfirmedBookingsResponse confirmedBookingsResponse) {
                        List<QueryServiceOuterClass.ConfirmedBooking> list = confirmedBookingsResponse.getConfirmedBookingsList();
                        generateConfirmedQueryFileContent(list, outPath);
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        System.out.println("Error\n");
                        latch.countDown();
                        System.err.println(throwable.getMessage());
                    }}, Runnable::run);
            }
            default -> logger.error("Invalid action");
        }

        try {
            System.out.println("Waiting for response...");
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void generateCapacityQueryFileContent(List<QueryServiceOuterClass.CapacitySuggestion> list, String outPath){
        StringBuilder sb = new StringBuilder();
        sb.append("Slot\t| Capacity  | Ride\n");
        for(QueryServiceOuterClass.CapacitySuggestion capacitySuggestion : list){
           sb.append(capacitySuggestion.getSlot().getValue()).append("\t| ")
                   .append(capacitySuggestion.getSuggestedCapacity().getValue()).append("\t\t\t| ")
                   .append(capacitySuggestion.getRideName().getValue()).append("\n");
        }
        createOutputFile(outPath, sb.toString());
    }

    private static void generateConfirmedQueryFileContent(List<QueryServiceOuterClass.ConfirmedBooking> list, String outPath){
        StringBuilder sb = new StringBuilder();
        sb.append("Slot\t| Visitor\t\t\t\t\t\t\t\t| Ride\n");
        for(QueryServiceOuterClass.ConfirmedBooking confirmedBooking : list){
            sb.append(confirmedBooking.getSlot().getValue()).append("\t| ")
                    .append(confirmedBooking.getVisitorId().getValue()).append("  | ")
                    .append(confirmedBooking.getRideName().getValue()).append("\n");
        }
        createOutputFile(outPath, sb.toString());
    }
}