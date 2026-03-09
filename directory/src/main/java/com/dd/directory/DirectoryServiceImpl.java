package com.dd.directory;

import com.dd.shared.DirectoryService;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class DirectoryServiceImpl extends UnicastRemoteObject implements DirectoryService {

    private List<String> clients = new ArrayList<>();

    protected DirectoryServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public synchronized void registerClient(String clientId) throws RemoteException {
        clients.add(clientId);
        System.out.println("Client registered: " + clientId);
    }

    @Override
    public List<String> getClients() throws RemoteException {
        return clients;
    }
}