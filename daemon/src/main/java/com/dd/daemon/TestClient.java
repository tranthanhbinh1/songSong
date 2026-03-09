package com.dd.daemon;

import com.dd.shared.DirectoryService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestClient {

    public static void main(String[] args) throws Exception {

        Registry registry = LocateRegistry.getRegistry("localhost", 1099);

        DirectoryService service =
                (DirectoryService) registry.lookup("DirectoryService");

        service.registerClient("client1");

        System.out.println(service.getClients());
    }
}