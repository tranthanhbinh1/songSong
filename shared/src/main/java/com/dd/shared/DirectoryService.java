package com.dd.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface DirectoryService extends Remote {

    void registerClient(String clientId, String host, int port) throws RemoteException;

    void registerFile(String clientId, String filename, long size) throws RemoteException;

    List<FileLocation> searchFile(String filename) throws RemoteException;

    void heartbeat(String clientId) throws RemoteException;
}