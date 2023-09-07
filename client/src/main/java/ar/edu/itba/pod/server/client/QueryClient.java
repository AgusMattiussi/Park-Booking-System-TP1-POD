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

        ManagedChannel channel = buildChannel("localhost", 50051);

        Map<String, String> argMap = parseArguments(args);
        final String serverAddress;
        final String action;
        final String day;
        final String outPath;

        try {
            System.out.println("Parsing command-line arguments");

            serverAddress = getArgumentValue(argMap, "serverAddress");;
            action = getArgumentValue(argMap, "action");
            day = getArgumentValue(argMap, "day");
            outPath = getArgumentValue(argMap, "outPath");

            System.out.println("Server Address: " + serverAddress);
            System.out.println("Action: " + action);
            System.out.println("Day: " + day);
            System.out.println("Output Path: " + outPath);

        } catch (RuntimeException e) {
            System.err.println("Error parsing command-line arguments: " + e.getMessage());
        } finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}