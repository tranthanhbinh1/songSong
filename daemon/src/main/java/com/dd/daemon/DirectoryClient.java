package com.dd.daemon;

import com.dd.shared.DirectoryService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.nio.file.Path;

public class DirectoryClient {
    private final DirectoryService service;

    public DirectoryClient(String host, int port) throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, port);
        this.service = (DirectoryService) registry.lookup("DirectoryService");
    }

    public void registerClient(String clientId, String host, int port) throws Exception {
        service.registerClient(clientId, host, port);
    }

    public void registerFile(String clientId, Path path, long size) throws Exception {
        service.registerFile(clientId, path.getFileName().toString(), size);
    }

    public void heartbeat(String clientId) throws Exception {
        service.heartbeat(clientId);
    }
}
