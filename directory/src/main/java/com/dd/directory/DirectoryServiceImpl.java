package com.dd.directory;

import com.dd.shared.ClientInfo;
import com.dd.shared.FileLocation;
import com.dd.shared.DirectoryService;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class DirectoryServiceImpl extends UnicastRemoteObject implements DirectoryService {

    Map<String, ClientInfo> clients = new ConcurrentHashMap<>();
    
    Map<String, List<String>> fileIndex = new ConcurrentHashMap<>();

    protected DirectoryServiceImpl() throws RemoteException {
        super();
        startCleanupThread();
    }

    @Override
    public synchronized void registerClient(String clientId, String host, int port) throws RemoteException {
        clients.put(clientId, new ClientInfo(clientId, host, port));
        System.out.println("Client registered: " + clientId);
    }

    @Override
    public synchronized void registerFile(String clientId, String filename, long size) throws RemoteException {
        fileIndex.putIfAbsent(filename, new ArrayList<>());
        List<String> providers = fileIndex.get(filename);

        if (!providers.contains(clientId)) {
            providers.add(clientId);
        }

        System.out.println(clientId + " provides file: " + filename);
    }

    @Override
    public List<ClientInfo> getClients() throws RemoteException {
        return new ArrayList<>(clients.values());
    }

    @Override
    public List<FileLocation> searchFile(String filename) throws RemoteException {
        List<FileLocation> results = new ArrayList<>();
        List<String> providers = fileIndex.get(filename);

        if (providers == null)
            return results;

        for (String clientId : providers) {
            ClientInfo c = clients.get(clientId);

            if (c != null) {
                results.add(new FileLocation(c.clientId, c.host, c.port));
            }
        }
        return results;
    }

    @Override
    public void heartbeat(String clientId) throws RemoteException {

        ClientInfo c = clients.get(clientId);

        if (c != null) {
            c.lastHeartbeat = System.currentTimeMillis();
        }
    }

    private void startCleanupThread() {
    new Thread(() -> {
        while (true) {
            long now = System.currentTimeMillis();

            // Remove client if no heartbeat sent for 30s
            clients.values().removeIf(c -> now - c.lastHeartbeat > 30000);
            
            fileIndex.values().forEach(list -> 
                list.removeIf(clientId -> !clients.containsKey(clientId))
            );

            System.out.println("Active clients: " + clients.keySet());

            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {}

        }

    }).start();
}
}