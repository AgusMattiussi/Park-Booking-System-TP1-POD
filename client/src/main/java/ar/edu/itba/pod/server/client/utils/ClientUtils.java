package ar.edu.itba.pod.server.client.utils;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;
import java.util.Map;

public final class ClientUtils {
    public static ManagedChannel buildChannel(String serverAddress, int port){
        return ManagedChannelBuilder.forAddress(serverAddress, port)
                .usePlaintext()
                .build();
    }

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

}
