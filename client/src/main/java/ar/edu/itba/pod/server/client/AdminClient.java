package ar.edu.itba.pod.server.client;

import ar.edu.itba.pod.server.client.utils.ClientUtils;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rideBooking.AdminParkServiceGrpc;

import java.util.Map;
import java.util.Objects;

import rideBooking.AdminParkServiceOuterClass.*;
import rideBooking.Models;

import static ar.edu.itba.pod.server.client.utils.ClientUtils.*;

public class AdminClient {
    private static final Logger logger = LoggerFactory.getLogger(AdminClient.class);

//     ./admin-cli -DserverAddress=10.6.0.1:50051 -Daction=addRide/addParkPass/addSlotCapacity -DrideName=rideName -DopenTime=00:00 -DcloseTime 23:59 -DslotMinutes=15 -Dday=100 -DvisitorId=1 -DpassType=2 -Dcapacity=20

    public static void main(String[] args) throws InterruptedException {
        logger.info("Admin Client Starting ...");

        Map<String, String> argMap = parseArguments(args);
        final String serverAddress = getArgumentValue(argMap, "serverAddress");
        final String action = getArgumentValue(argMap, "action");

        ManagedChannel channel = ClientUtils.buildChannel(serverAddress);

        AdminParkServiceGrpc.AdminParkServiceBlockingStub stub = AdminParkServiceGrpc.newBlockingStub(channel);

        if(Objects.equals(action, "addParkPass")){
            final String visitorId = getArgumentValue(argMap, "visitorId");
            final String passType = getArgumentValue(argMap, "passType");
            final String day = getArgumentValue(argMap, "day");

            AddPassRequest addPassRequest = AddPassRequest.newBuilder().setPassTypeValue(Integer.parseInt(passType)).setVisitorId(visitorId).setValidDay(Integer.parseInt(day)).build();
            String return_info = "ParkPass " + passType + " for visitor "+ visitorId;
            try {
                BoolValue created = stub.addPassToPark(addPassRequest);
                if(created.getValue()){
                    System.out.println(return_info + " was succesfully created.");
                }else {
                    System.out.println(return_info + " wasn't  created.");
                }
            } catch (Exception e) {
                System.out.println("Cannot add " + return_info + '.');
            }
       }else{
            final String rideName = getArgumentValue(argMap, "rideName");

            if (Objects.equals(action, "addRide")){
                final String openTime = getArgumentValue(argMap, "openTime");
                final String closeTime = getArgumentValue(argMap, "closeTime");
                final String slotMinutes = getArgumentValue(argMap, "slotMinutes");

                Models.RideTime rideTime = Models.RideTime.newBuilder().setClose(closeTime).setOpen(openTime).build();
                AddRideRequest addRideRequest = AddRideRequest.newBuilder().setRideName(rideName).setRideTime(rideTime).setSlotMinutes(Integer.parseInt(slotMinutes)).build();

                try {

                    Int32Value ride_id = stub.addRide(addRideRequest);
                    System.out.println("Ride " + rideName + " was succesfully created.");
                } catch (Exception e) {
                    System.out.println("Cannot add ride called " + rideName + '.');
                }
            }else{
                final String day = getArgumentValue(argMap, "day");
                final String capacity = getArgumentValue(argMap, "capacity");

                AddSlotCapacityRequest addSlotCapacityRequest = AddSlotCapacityRequest.newBuilder().setRideName(rideName).setSlotCapacity(Integer.parseInt(capacity)).setValidDay(Integer.parseInt(day)).build();

                try {
                    SlotCapacityResponse slotCapacityResponse = stub.addSlotCapacity(addSlotCapacityRequest);
                    String response = "Accepted\t| Relocated\t| Cancelled\n" +
                            slotCapacityResponse.getAcceptedAmount() + "\t|" + slotCapacityResponse.getRelocatedAmount() + "\t|" + slotCapacityResponse.getCancelledAmount();
                    System.out.println(response);
                } catch (Exception e) {
                    System.out.println("Cannot add slot capacity for ride called " + rideName + '.');
                }
            }
        }
    }


}