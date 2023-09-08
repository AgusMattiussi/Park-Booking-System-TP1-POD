package ar.edu.itba.pod.server.client.utils;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ClientUtils {
    private final static Logger logger = LoggerFactory.getLogger(ClientUtils.class);
    public final static String SERVER_ADDRESS = "serverAddress";
    public final static String ACTION_NAME = "action";
    public final static String RIDE_NAME = "rideName";
    public final static String OPEN_TIME = "openTime";
    public final static String CLOSE_TIME = "closeTime";
    public final static String SLOT_MINUTES = "slotMinutes";
    public final static String DAY = "day";
    public final static String VISITOR = "visitor";
    public final static String PASS_TYPE = "passType";
    public final static String CAPACITY = "capacity";
    public final static String OUTPATH = "outPath";

    public static <T> Optional<T> getProperty(String name, Supplier<String> errorMsg, Function<String, T> converter){
        final String prop = System.getProperty(name);
        if(prop == null){
            final String msg = errorMsg.get();
            logger.error(msg);
            return Optional.empty();
        }
        try {
            return Optional.of(converter.apply(prop));
        }catch (ClassCastException e){
            logger.error("Cannot convert " + prop + " for " + name);
            System.out.println("Invalid argument " + prop + " for " + name);
        }
        return Optional.empty();
    }

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
            System.err.println("Error creating output file");
        }
    }

}
