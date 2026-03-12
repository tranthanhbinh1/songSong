package com.dd.download;

import com.dd.shared.DirectoryService;
import com.dd.shared.FileLocation;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

public class DirectoryClient {
    private final DirectoryService service;

    public DirectoryClient(String host, int port) throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, port);
        this.service = (DirectoryService) registry.lookup("DirectoryService");
    }

    public List<FileLocation> searchFile(String filename) throws Exception {
        return service.searchFile(filename);
    }
}
