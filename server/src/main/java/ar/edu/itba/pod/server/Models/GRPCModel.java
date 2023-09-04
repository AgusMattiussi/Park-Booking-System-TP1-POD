package ar.edu.itba.pod.server.Models;

public interface GRPCModel<T> {
    T convertToGRPC();

}
