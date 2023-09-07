package ar.edu.itba.pod.server.client;

import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static ar.edu.itba.pod.server.client.utils.ClientUtils.*;

public class QueryClient {
    private static final Logger logger = LoggerFactory.getLogger(QueryClient.class);

//     ./query-cli -DserverAddress=10.6.0.1:50051 -Daction=capacity -Dday=100 -DoutPath=query1.csv
    public static void main(String[] args) throws InterruptedException {
        logger.info("Query Client Starting ...");

        Map<String, String> argMap = parseArguments(args);
        final String serverAddress = getArgumentValue(argMap, "serverAddress");
        final String action = getArgumentValue(argMap, "action");;
        final String day = getArgumentValue(argMap, "day");
        final String outPath = getArgumentValue(argMap, "outPath");

        ManagedChannel channel = buildChannel(serverAddress);

        try {
            switch(action){
                case "capacity" -> {
                    System.out.println("capacity query");
                }
                case "confirmed" -> {
                    System.out.println("confirmed query");
                }
            }
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        } finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}