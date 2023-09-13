package ar.edu.itba.pod.server.Models.requests;

import rideBooking.QueryServiceOuterClass;

public class QueryDayRequestModel {

    private final int day;

    public QueryDayRequestModel(int day) {
        if(day < 1 || day > 365)
            throw new IllegalArgumentException("Day must be between 1 and 365");

        this.day = day;
    }

    public int getDay() {
        return day;
    }

    public static QueryDayRequestModel fromQueryDayRequest(QueryServiceOuterClass.QueryDayRequest request){
        if(!request.hasDayOfYear())
            throw new IllegalArgumentException("Day of year must be specified");

        return new QueryDayRequestModel(request.getDayOfYear().getValue());
    }
}
