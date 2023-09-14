package ar.edu.itba.pod.client.utils.callbacks;

import ar.edu.itba.pod.client.utils.ClientUtils;
import org.slf4j.Logger;
import rideBooking.QueryServiceOuterClass;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ConfirmedBookingsResponseFutureCallback extends CustomFutureCallback<QueryServiceOuterClass.ConfirmedBookingsResponse>{

    private final String outPath;

    public ConfirmedBookingsResponseFutureCallback(Logger logger, CountDownLatch latch, String outPath) {
        super(logger, latch);
        this.outPath = outPath;
    }

    @Override
    public void onSuccess(QueryServiceOuterClass.ConfirmedBookingsResponse confirmedBookingsResponse) {
        List<QueryServiceOuterClass.ConfirmedBooking> list = confirmedBookingsResponse.getConfirmedBookingsList();
        generateConfirmedQueryFileContent(list, outPath);
        getLatch().countDown();
    }

    private static void generateConfirmedQueryFileContent(List<QueryServiceOuterClass.ConfirmedBooking> list, String outPath){
        if(list.isEmpty()) {
            System.out.println("No confirmed bookings for specified day");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Slot\t| Visitor\t\t\t\t\t\t\t\t| Ride\n");
        for(QueryServiceOuterClass.ConfirmedBooking confirmedBooking : list){
            sb.append(confirmedBooking.getSlot()).append("\t| ")
                    .append(confirmedBooking.getVisitorId()).append("  | ")
                    .append(confirmedBooking.getRideName()).append("\n");
        }
        ClientUtils.createOutputFile(outPath, sb.toString());
    }
}
