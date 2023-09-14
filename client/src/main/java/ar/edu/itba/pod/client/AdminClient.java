package ar.edu.itba.pod.client;

import ar.edu.itba.pod.client.utils.ClientUtils;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
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

import rideBooking.AdminParkServiceOuterClass.*;
import rideBooking.Models;

import static ar.edu.itba.pod.client.utils.ClientUtils.*;

public class AdminClient {
    private static final Logger logger = LoggerFactory.getLogger(AdminClient.class);

//     ./admin-cli -DserverAddress=10.6.0.1:50051 -Daction=rides/tickets/slots -Dride=ride -Dday=100 -Dcapacity=20 -DinPath="excel.csv"

    public static void main(String[] args) throws InterruptedException {
        logger.info("Admin Client Starting ...");

        Map<String, String> argMap = parseArguments(args);
        final String serverAddress = getArgumentValue(argMap, SERVER_ADDRESS);
        final String action = getArgumentValue(argMap, ACTION_NAME);;

        ManagedChannel channel = ClientUtils.buildChannel(serverAddress);

        AdminParkServiceGrpc.AdminParkServiceBlockingStub stub = AdminParkServiceGrpc.newBlockingStub(channel);
        if(Objects.equals(action, "slots")){
            final String rideName = getArgumentValue(argMap, RIDE_NAME);
            final String day = getArgumentValue(argMap, DAY);
            final String capacity = getArgumentValue(argMap, CAPACITY);

            AddSlotCapacityRequest addSlotCapacityRequest = AddSlotCapacityRequest.newBuilder().setRideName(rideName).setSlotCapacity(Integer.parseInt(capacity)).setValidDay(Integer.parseInt(day)).build();

            try {
                SlotCapacityResponse slotCapacityResponse = stub.addSlotCapacity(addSlotCapacityRequest);
                String response = String.format("""
                                Loaded capacity of %s for %s on day %s
                                %s bookings confirmed without changes
                                %s bookings relocated
                                %s bookings cancelled
                                """,
                        capacity, rideName, day,
                        slotCapacityResponse.getAcceptedAmount(),
                        slotCapacityResponse.getRelocatedAmount(),
                        slotCapacityResponse.getCancelledAmount());
                System.out.println(response);
            } catch (Exception e) {
                System.out.printf("Cannot add slot capacity for ride called %s on day %s.%n", rideName, day);
                System.out.println(e.getMessage());
            }

        }else{
            final String inPath = ClientUtils.getArgumentValue(argMap, INPUT_PATH);
            List<String[]> csvData = getCSVData(inPath);
            int added = 0;
            int couldNotAdd = 0;
            if (Objects.equals(action, "rides")){
                for (String[] data : csvData) {
//                    data => name;hoursFrom;hoursTo;slotGap
                    final String rideName = data[0];
                    Models.RideTime rideTime = Models.RideTime.newBuilder().setOpen(data[1]).setClose(data[2]).build();
                    AddRideRequest addRideRequest = AddRideRequest.newBuilder().setRideName(rideName).setRideTime(rideTime).setSlotMinutes(Integer.parseInt(data[3])).build();
                    try {
                        BoolValue created = stub.addRide(addRideRequest);
                        if (created.getValue()){
                            added++;
                        }else {
                            couldNotAdd++;
                        }
                    } catch (Exception e) {
                        couldNotAdd++;
                    }
                }
            }else{
//                tickets
                for (String[] data : csvData) {
//                    data => visitorId;passType;dayOfYear
                    int passEnumInfo = Models.PassTypeEnum.valueOf(data[1]).getNumber();
                    AddPassRequest addPassRequest = AddPassRequest.newBuilder().setVisitorId(data[0]).setPassTypeValue(passEnumInfo).setValidDay(Integer.parseInt(data[2])).build();
                    try {
                        BoolValue created = stub.addPassToPark(addPassRequest);
                        if (created.getValue()){
                            added++;
                        }else {
                            couldNotAdd++;
                        }
                    } catch (Exception e) {
                        couldNotAdd++;
                    }
                }
            }
            String s = Objects.equals(action, "rides") ? " attractions" : " passes";

            if(couldNotAdd>0){
                System.out.printf("Cannot add %d %s\n", couldNotAdd, s);
            }
            System.out.printf("%d %s added\n", added, s);
        }
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