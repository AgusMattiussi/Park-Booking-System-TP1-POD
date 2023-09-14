package ar.edu.itba.pod.server;

import ar.edu.itba.pod.server.exceptions.GlobalExceptionHandlerInterceptor;
import ar.edu.itba.pod.server.server.AdminParkServer;
import ar.edu.itba.pod.server.server.NotifyService;
import ar.edu.itba.pod.server.server.QueryService;
import ar.edu.itba.pod.server.server.RideBookingService;
import io.grpc.BindableService;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Function;

public class Server {

    private static final Function<BindableService, ServerServiceDefinition> handledService =
            service -> ServerInterceptors.intercept(service, new GlobalExceptionHandlerInterceptor());
    private static Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) throws InterruptedException, IOException {
        logger.info(" Server Starting ...");

        int port = 50051;
        io.grpc.Server server = ServerBuilder.forPort(port)
                .addService(handledService.apply(new RideBookingService()))
                .addService(handledService.apply(new QueryService()))
                .addService(handledService.apply(new NotifyService()))
                .addService(handledService.apply(new AdminParkServer()))
                .build();
        server.start();
        logger.info("Server started, listening on " + port);
        server.awaitTermination();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gRPC server since JVM is shutting down");
            server.shutdown();
            logger.info("Server shut down");
        }));
    }}
