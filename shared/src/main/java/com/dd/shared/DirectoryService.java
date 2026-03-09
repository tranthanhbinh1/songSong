package com.dd.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface DirectoryService extends Remote {

    void registerClient(String clientId) throws RemoteException;

    List<String> getClients() throws RemoteException;
}