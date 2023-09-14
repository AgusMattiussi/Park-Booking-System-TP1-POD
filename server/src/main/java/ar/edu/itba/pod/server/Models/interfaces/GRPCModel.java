package ar.edu.itba.pod.server.Models.interfaces;

public interface GRPCModel<T> {
    T convertToGRPC();

}
