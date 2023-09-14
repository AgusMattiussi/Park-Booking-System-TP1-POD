package ar.edu.itba.pod.server.exceptions;

import com.google.rpc.Code;
import io.grpc.*;
import io.grpc.protobuf.StatusProto;
import jdk.jshell.spi.ExecutionControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class GlobalExceptionHandlerInterceptor implements ServerInterceptor {
    private final static Logger logger = LoggerFactory.getLogger(GlobalExceptionHandlerInterceptor.class);
    @Override
    public <T, R> ServerCall.Listener<T> interceptCall(
            ServerCall<T, R> serverCall, Metadata headers, ServerCallHandler<T, R> serverCallHandler) {
        ServerCall.Listener<T> delegate = serverCallHandler.startCall(serverCall, headers);
        return new ExceptionHandler<>(delegate, serverCall, headers);
    }

    private static class ExceptionHandler<T, R> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<T> {

        private final ServerCall<T, R> delegate;
        private final Metadata headers;

        ExceptionHandler(ServerCall.Listener<T> listener, ServerCall<T, R> serverCall, Metadata headers) {
            super(listener);
            this.delegate = serverCall;
            this.headers = headers;
        }

        @Override
        public void onHalfClose() {
            try {
                super.onHalfClose();
            } catch (RuntimeException ex) {
                handleException(ex, delegate, headers);
            }
        }

        private static final Map<Class<? extends Throwable>, Code> errorCodesByException = Map.ofEntries(
                Map.entry(ExecutionControl.NotImplementedException.class, Code.UNIMPLEMENTED),
                Map.entry(AlreadyExistsException.class, Code.ALREADY_EXISTS),
                Map.entry(RideNotFoundException.class, Code.NOT_FOUND),
                Map.entry(SlotNotFoundException.class, Code.NOT_FOUND),
                Map.entry(InvalidPassTypeException.class, Code.INVALID_ARGUMENT),
                Map.entry(SlotCapacityException.class, Code.INVALID_ARGUMENT),
                Map.entry(InvalidTimeException.class, Code.CANCELLED),
                Map.entry(RuntimeException.class, Code.INTERNAL),
                Map.entry(AlreadyConfirmedException.class, Code.ALREADY_EXISTS),
                Map.entry(PassNotFoundException.class, Code.NOT_FOUND),
                Map.entry(ReservationLimitException.class, Code.INVALID_ARGUMENT),
                Map.entry(ReservationNotFoundException.class, Code.NOT_FOUND)
        );


        private void handleException(RuntimeException exception, ServerCall<T, R> serverCall, Metadata headers) {
            Throwable error = exception;
            logger.error("Unexpected error:", error);
            if (!errorCodesByException.containsKey(error.getClass())) {
                // Porque en el ConcurrencyLock wrappeamos la excepción necesitamos preguntar por la causa.
                error = error.getCause();
                if (error == null || !errorCodesByException.containsKey(error.getClass())) {
                    // Una excepción NO esperada.
                    serverCall.close(Status.UNKNOWN, headers);
                    return;
                }
            }
            // Una excepción esperada.
            com.google.rpc.Status rpcStatus = com.google.rpc.Status.newBuilder()
                    .setCode(errorCodesByException.get(error.getClass()).getNumber())
                    .setMessage(error.getMessage())
                    .build();
            StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(rpcStatus);
            Status newStatus = Status.fromThrowable(statusRuntimeException);
            serverCall.close(newStatus, headers);
        }
    }

}