package com.dd.directory;

import com.dd.shared.ClientInfo;
import com.dd.shared.FileLocation;
import com.dd.shared.DirectoryService;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class DirectoryServiceImpl extends UnicastRemoteObject implements DirectoryService {

    private final Map<String, ClientInfo> clients = new ConcurrentHashMap<>();
    private final Map<String, FileRecord> fileIndex = new ConcurrentHashMap<>();
    private final long heartbeatTimeoutMillis;
    private final long cleanupIntervalMillis;

    public DirectoryServiceImpl() throws RemoteException {
        this(10_000L, 2_000L);
    }

    public DirectoryServiceImpl(long heartbeatTimeoutMillis, long cleanupIntervalMillis) throws RemoteException {
        super();
        this.heartbeatTimeoutMillis = heartbeatTimeoutMillis;
        this.cleanupIntervalMillis = cleanupIntervalMillis;
        startCleanupThread();
    }

    @Override
    public synchronized void registerClient(String clientId, String host, int port) throws RemoteException {
        clients.put(clientId, new ClientInfo(clientId, host, port));
        System.out.println("Client registered: " + clientId);
    }

    @Override
    public synchronized void registerFile(String clientId, String filename, long size) throws RemoteException {
        FileRecord fileRecord = fileIndex.get(filename);
        if (fileRecord == null) {
            fileRecord = new FileRecord(size);
            fileIndex.put(filename, fileRecord);
        } else if (fileRecord.size != size) {
            System.err.println("Ignoring provider " + clientId + " for file " + filename
                    + " due to size mismatch. Expected " + fileRecord.size + " but got " + size + ".");
            return;
        }

        fileRecord.providers.add(clientId);
        System.out.println(clientId + " provides file: " + filename);
    }

    @Override
    public synchronized List<FileLocation> searchFile(String filename) throws RemoteException {
        List<FileLocation> results = new ArrayList<>();
        FileRecord fileRecord = fileIndex.get(filename);

        if (fileRecord == null)
            return results;

        for (String clientId : fileRecord.providers) {
            ClientInfo c = clients.get(clientId);

            if (c != null) {
                results.add(new FileLocation(c.clientId, c.host, c.port, fileRecord.size));
            }
        }
        return results;
    }

    @Override
    public synchronized void heartbeat(String clientId) throws RemoteException {

        ClientInfo c = clients.get(clientId);

        if (c != null) {
            c.lastHeartbeat = System.currentTimeMillis();
        }
    }

    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                long now = System.currentTimeMillis();

                synchronized (DirectoryServiceImpl.this) {
                    clients.values().removeIf(c -> now - c.lastHeartbeat > heartbeatTimeoutMillis);
                    fileIndex.entrySet().removeIf(entry -> {
                        entry.getValue().providers.removeIf(clientId -> !clients.containsKey(clientId));
                        return entry.getValue().providers.isEmpty();
                    });
                }

                System.out.println("Active clients: " + clients.keySet());

                try {
                    Thread.sleep(cleanupIntervalMillis);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "directory-cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    private static final class FileRecord {
        private final long size;
        private final LinkedHashSet<String> providers = new LinkedHashSet<>();

        private FileRecord(long size) {
            this.size = size;
        }
    }
}
