package com.dd.directory;

import com.dd.shared.DirectoryService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class DirectoryServer {

    public static void main(String[] args) throws Exception {

        DirectoryService service = new DirectoryServiceImpl();

        Registry registry = LocateRegistry.createRegistry(1099);

        registry.rebind("DirectoryService", service);

        System.out.println("Directory RMI server running...");
    }
}