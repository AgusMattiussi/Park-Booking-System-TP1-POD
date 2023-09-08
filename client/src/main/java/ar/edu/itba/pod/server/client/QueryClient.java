package ar.edu.itba.pod.server.client;

import ar.edu.itba.pod.server.client.utils.ClientUtils;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rideBooking.QueryServiceOuterClass;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static ar.edu.itba.pod.server.client.utils.ClientUtils.*;

public class QueryClient {
    private static final Logger logger = LoggerFactory.getLogger(QueryClient.class);

//     ./query-cli -DserverAddress=10.6.0.1:50051 -Daction=capacity/confirmed -Dday=100 -DoutPath=query1.txt
    public static void main(String[] args) throws InterruptedException {
        logger.info("Query Client Starting ...");

        Map<String, String> argMap = parseArguments(args);

        final String serverAddress = getArgumentValue(argMap, SERVER_ADDRESS);
        final String action = getArgumentValue(argMap, ACTION_NAME);;
        final String day = getArgumentValue(argMap, DAY);
        final String outPath = getArgumentValue(argMap, OUTPATH);

        ManagedChannel channel = buildChannel(serverAddress);

        try {
            switch(action){
                case "capacity" -> {
                    logger.info("Capacity Suggestion Query\n");

                    List<QueryServiceOuterClass.CapacitySuggestion> list = new LinkedList<>();

                    list.add(QueryServiceOuterClass.CapacitySuggestion.newBuilder()
                            .setRideName(StringValue.of("ride1"))
                            .setSuggestedCapacity(Int32Value.of(1))
                            .setSlot(StringValue.of("10:00"))
                            .build());

                    list.add(QueryServiceOuterClass.CapacitySuggestion.newBuilder()
                            .setRideName(StringValue.of("ride2"))
                            .setSuggestedCapacity(Int32Value.of(1))
                            .setSlot(StringValue.of("12:00"))
                            .build());

                    generateCapacityQueryFileContent(list, outPath);

                }
                case "confirmed" -> {
                    logger.info("Confirmed Bookings Query\n");

                    List<QueryServiceOuterClass.ConfirmedBooking> list = new LinkedList<>();

                    list.add(QueryServiceOuterClass.ConfirmedBooking.newBuilder()
                            .setRideName(StringValue.of("ride1"))
                            .setVisitorId(StringValue.of("ca286ef0-162a-42fd-b9ea-60166ff0a593"))
                            .setSlot(StringValue.of("10:00"))
                            .build());

                    list.add(QueryServiceOuterClass.ConfirmedBooking.newBuilder()
                            .setRideName(StringValue.of("ride2"))
                            .setVisitorId(StringValue.of("ca286ef0-162a-42fd-b9ea-60166ff0a593"))
                            .setSlot(StringValue.of("12:00"))
                            .build());

                    generateConfirmedQueryFileContent(list, outPath);

                }
            }
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        } finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
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