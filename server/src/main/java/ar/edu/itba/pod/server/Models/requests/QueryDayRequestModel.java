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
        return new QueryDayRequestModel(Integer.parseInt(request.getDayOfYear()));
    }
}
