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

//     ./admin-cli -DserverAddress=10.6.0.1:50051 -Daction=addRide/addParkPass/addSlotCapacity -DrideName=rideName -DopenTime=00:00 -DcloseTime 23:59 -DslotMinutes=15 -Dday=100 -Dvisitor=1 -DpassType=2 -Dcapacity=20

    public static void main(String[] args) throws InterruptedException {
        logger.info("Admin Client Starting ...");

        Map<String, String> argMap = parseArguments(args);

        final String serverAddress = ClientUtils.getProperty(ClientUtils.SERVER_ADDRESS, () -> "Missing server address.", x -> x).orElseThrow();
        final String action = ClientUtils.getProperty(ClientUtils.ACTION_NAME, () -> "Missing action name.", x -> x).orElseThrow();

        ManagedChannel channel = ClientUtils.buildChannel(serverAddress);

        AdminParkServiceGrpc.AdminParkServiceBlockingStub stub = AdminParkServiceGrpc.newBlockingStub(channel);

        if(Objects.equals(action, "addParkPass")){
            final String visitorId = ClientUtils.getProperty(ClientUtils.VISITOR, () -> "Missing visitor ID.", x -> x).orElseThrow();
            final String passType = ClientUtils.getProperty(ClientUtils.PASS_TYPE, () -> "Missing park pass type.", x -> x).orElseThrow();
            final String day = ClientUtils.getProperty(ClientUtils.DAY, () -> "Missing day of year.", x -> x).orElseThrow();

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
            final String rideName = ClientUtils.getProperty(ClientUtils.RIDE_NAME, () -> "Missing day of year.", x -> x).orElseThrow();
            if (Objects.equals(action, "addRide")){
                final String openTime = ClientUtils.getProperty(ClientUtils.OPEN_TIME, () -> "Missing open ride time.", x -> x).orElseThrow();
                final String closeTime = ClientUtils.getProperty(ClientUtils.CLOSE_TIME, () -> "Missing close ride time.", x -> x).orElseThrow();
                final String slotMinutes = ClientUtils.getProperty(ClientUtils.SLOT_MINUTES, () -> "Missing slot minutes.", x -> x).orElseThrow();
                Models.RideTime rideTime = Models.RideTime.newBuilder().setClose(closeTime).setOpen(openTime).build();
                AddRideRequest addRideRequest = AddRideRequest.newBuilder().setRideName(rideName).setRideTime(rideTime).setSlotMinutes(Integer.parseInt(slotMinutes)).build();

                try {
                    Int32Value ride_id = stub.addRide(addRideRequest);
                    System.out.println("Ride " + rideName + " was succesfully created.");
                } catch (Exception e) {
                    System.out.println("Cannot add ride called " + rideName + '.');
                }
            }else{
                final String day = ClientUtils.getProperty(ClientUtils.DAY, () -> "Missing day of year.", x -> x).orElseThrow();
                final String capacity = ClientUtils.getProperty(ClientUtils.CAPACITY, () -> "Missing slot capacity.", x -> x).orElseThrow();
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