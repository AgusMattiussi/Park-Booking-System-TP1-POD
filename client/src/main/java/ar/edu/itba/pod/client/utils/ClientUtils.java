package ar.edu.itba.pod.client.utils;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rideBooking.Models;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
public final class ClientUtils {
    private final static Logger logger = LoggerFactory.getLogger(ClientUtils.class);
    public final static String INPUT_PATH = "inPath";
    public final static String SERVER_ADDRESS = "serverAddress";
    public final static String ACTION_NAME = "action";
    public final static String RIDE_NAME = "ride";
    public final static String ATTRACTION = "attraction";
    public final static String OPEN_TIME = "openTime";
    public final static String CLOSE_TIME = "closeTime";
    public final static String SLOT_MINUTES = "slotGap";
    public final static String DAY = "day";
    public final static String VISITOR_ID = "visitor";
    public final static String PASS_TYPE = "passType";
    public final static String CAPACITY = "capacity";
    public final static String OUTPATH = "outPath";
    public static final String BOOKING_SLOT = "slot";
    public static final String BOOKING_SLOT_TO = "slotTo";

    public static ManagedChannel buildChannel(String serverAddress){
        return ManagedChannelBuilder.forTarget(serverAddress)
                .usePlaintext()
                .build();
    }

    //TODO: manejar errores?
    public static Map<String, String> parseArguments(String[] args) {
        Map<String, String> argMap = new HashMap<>();
        for (String arg : args) {
            String[] parts = arg.substring(2).split("=");
            if (parts.length == 2) {
                argMap.put(parts[0], parts[1]);
            }
        }
        return argMap;
    }

    public static String getArgumentValue(Map<String, String> argMap, String key) {
        return argMap.get(key);
    }

    public static void createOutputFile(String outPath, String content) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outPath));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            logger.error("Error creating output file");
        }
    }
}

