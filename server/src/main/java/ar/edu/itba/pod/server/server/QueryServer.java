package ar.edu.itba.pod.server.server;

import io.grpc.stub.StreamObserver;
import rideBooking.QueryServiceGrpc;
import rideBooking.QueryServiceOuterClass;

public class QueryServer extends QueryServiceGrpc.QueryServiceImplBase{
    @Override
    public void queryCapacitySuggestion(QueryServiceOuterClass.QueryDayRequest request, StreamObserver<QueryServiceOuterClass.CapacitySuggestionResponse> responseObserver) {
        super.queryCapacitySuggestion(request, responseObserver);
    }

    @Override
    public void queryConfirmedBookings(QueryServiceOuterClass.QueryDayRequest request, StreamObserver<QueryServiceOuterClass.ConfirmedBookingsResponse> responseObserver) {
        super.queryConfirmedBookings(request, responseObserver);
    }
}
