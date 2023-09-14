package ar.edu.itba.pod.client.utils.callbacks;

import ar.edu.itba.pod.client.utils.ClientUtils;
import org.slf4j.Logger;
import rideBooking.QueryServiceOuterClass;

import java.util.List;
import java.util.concurrent.CountDownLatch;


public class CapacitySuggestionResponseFutureCallback extends CustomFutureCallback<QueryServiceOuterClass.CapacitySuggestionResponse>{

    public final String outPath;

    public CapacitySuggestionResponseFutureCallback(Logger logger, CountDownLatch latch, String outPath) {
        super(logger, latch);
        this.outPath = outPath;
    }

    @Override
    public void onSuccess(QueryServiceOuterClass.CapacitySuggestionResponse capacitySuggestionResponse) {
        List<QueryServiceOuterClass.CapacitySuggestion> list = capacitySuggestionResponse.getCapacitySuggestionsList();
        generateCapacityQueryFileContent(list, outPath);
        getLatch().countDown();
    }

    private static void generateCapacityQueryFileContent(List<QueryServiceOuterClass.CapacitySuggestion> list, String outPath){
        if(list.isEmpty()) {
            System.out.println("No rides in the park");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Slot\t| Capacity  | Ride\n");
        for(QueryServiceOuterClass.CapacitySuggestion capacitySuggestion : list){
           sb.append(capacitySuggestion.getSlot()).append("\t| ")
                   .append(capacitySuggestion.getSuggestedCapacity()).append("\t\t\t| ")
                   .append(capacitySuggestion.getRideName()).append("\n");
        }
        ClientUtils.createOutputFile(outPath, sb.toString());
    }
}
