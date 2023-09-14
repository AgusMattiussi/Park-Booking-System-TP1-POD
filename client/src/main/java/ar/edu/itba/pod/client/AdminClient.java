package ar.edu.itba.pod.client;

import ar.edu.itba.pod.client.utils.ClientUtils;
import ar.edu.itba.pod.client.utils.callbacks.SlotCapacityResponseFutureCallback;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.BoolValue;
import com.opencsv.*;

import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rideBooking.AdminParkServiceGrpc;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import rideBooking.AdminParkServiceOuterClass.*;
import rideBooking.Models;

import static ar.edu.itba.pod.client.utils.ClientUtils.*;

public class AdminClient {
    private static final Logger logger = LoggerFactory.getLogger(AdminClient.class);
    private static final CountDownLatch latch = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException {
        logger.info("Admin Client Starting ...");

        Map<String, String> argMap = parseArguments(args);
        final String serverAddress = getArgumentValue(argMap, SERVER_ADDRESS);
        final String action = getArgumentValue(argMap, ACTION_NAME);;

        ManagedChannel channel = ClientUtils.buildChannel(serverAddress);

        AdminParkServiceGrpc.AdminParkServiceFutureStub stub = AdminParkServiceGrpc.newFutureStub(channel);


        final AtomicInteger added = new AtomicInteger(0);
        final AtomicInteger couldNotAdd = new AtomicInteger(0);

        if(Objects.equals(action, "slots")){
            final String rideName = getArgumentValue(argMap, RIDE_NAME);
            final String day = getArgumentValue(argMap, DAY);
            final String capacity = getArgumentValue(argMap, CAPACITY);

            AddSlotCapacityRequest addSlotCapacityRequest = AddSlotCapacityRequest.newBuilder().setRideName(rideName).setSlotCapacity(Integer.parseInt(capacity)).setValidDay(Integer.parseInt(day)).build();

            ListenableFuture<SlotCapacityResponse> result = stub.addSlotCapacity(addSlotCapacityRequest);
            Futures.addCallback(result, new SlotCapacityResponseFutureCallback(logger, latch, rideName, day, capacity),
                    Runnable::run);

        }else{
            final String inPath = ClientUtils.getArgumentValue(argMap, INPUT_PATH);
            List<String[]> csvData = getCSVData(inPath);
            for (String[] data : csvData) {
                ListenableFuture<BoolValue> result;
                if (Objects.equals(action, "rides")) {
                    final String rideName = data[0];
                    Models.RideTime rideTime = Models.RideTime.newBuilder().setOpen(data[1]).setClose(data[2]).build();
                    AddRideRequest addRideRequest = AddRideRequest.newBuilder().setRideName(rideName).setRideTime(rideTime).setSlotMinutes(Integer.parseInt(data[3])).build();
                    result = stub.addRide(addRideRequest);
                }else {
                    int passEnumInfo = Models.PassTypeEnum.valueOf(data[1]).getNumber();
                    AddPassRequest addPassRequest = AddPassRequest.newBuilder().setVisitorId(data[0]).setPassTypeValue(passEnumInfo).setValidDay(Integer.parseInt(data[2])).build();
                    result = stub.addPassToPark(addPassRequest);
                }
                couldAdd(added, couldNotAdd, result);
            }
        }

        try {
            logger.info("Waiting for response ...");
            latch.await(); // Espera hasta que la operación esté completa
            if(!Objects.equals(action, "slots")){
                String s = Objects.equals(action, "rides") ? " attractions" : " passes";
                int couldNotAddNum = couldNotAdd.get();
                if(couldNotAddNum >0){
                    System.out.printf("Cannot add %d %s\n", couldNotAddNum, s);
                }
                System.out.printf("%d %s added\n", added.get(), s);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(e.getMessage());
        }
    }

    private static void couldAdd(AtomicInteger added, AtomicInteger couldNotAdd, ListenableFuture<BoolValue> result) {
        Futures.addCallback(result, new FutureCallback<>() {
            @Override
            public void onSuccess(BoolValue created) {
                if (created.getValue()){
                    added.incrementAndGet();
                }else {
                    couldNotAdd.incrementAndGet();
                }
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable throwable) {
                latch.countDown();
                logger.error(throwable.getMessage());
            }

        }, Runnable::run);
    }

    private static List<String[]> getCSVData(String inPath) {
        FileReader filereader = null;
        try {
            filereader = new FileReader(inPath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(String.format("File %s not found", inPath));
        }

        CSVParser parser = new CSVParserBuilder()
                .withSeparator(';')
                .build();

        try(CSVReader csvReader = new CSVReaderBuilder(filereader)
                .withSkipLines(1) //first lines are titles => skip them
                .withCSVParser(parser)
                .build()) {
            return csvReader.readAll();
        } catch (IOException e) {
            throw new RuntimeException(String.format("There was an error reading the file %s", inPath));
        }
    }


}