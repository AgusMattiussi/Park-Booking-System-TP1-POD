package ar.edu.itba.pod.grpc.server;

import ar.edu.itba.pod.grpc.query.CapacitySuggestionResponse;
import ar.edu.itba.pod.grpc.query.ConfirmedBookingsResponse;
import ar.edu.itba.pod.grpc.query.QueryDayRequest;
import ar.edu.itba.pod.grpc.query.QueryServiceGrpc;
import io.grpc.stub.StreamObserver;

public class QueryServer extends QueryServiceGrpc.QueryServiceImplBase{
    @Override
    public void queryCapacitySuggestion(QueryDayRequest request, StreamObserver<CapacitySuggestionResponse> responseObserver) {
        super.queryCapacitySuggestion(request, responseObserver);
    }

    @Override
    public void queryConfirmedBookings(QueryDayRequest request, StreamObserver<ConfirmedBookingsResponse> responseObserver) {
        super.queryConfirmedBookings(request, responseObserver);
    }
}
