package com.dd.daemon;

import com.dd.shared.DirectoryService;
import com.dd.shared.FileLocation;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

public class TestClient {

    public static void main(String[] args) throws Exception {

        Registry registry = LocateRegistry.getRegistry("localhost", 1099);

        DirectoryService service = (DirectoryService) registry.lookup("DirectoryService");

        // change client id if creating new terminal
        String clientId = "client1";
        service.registerClient(clientId, "localhost", 5000);
        service.registerFile(clientId, "movie.mp4", 1000);

        // for testing purposes, change if you want to see a list of clients
        // service.registerClient("client2", "localhost", 5001);
        // service.registerFile("client2", "movie.mp4", 1000);

        // List<FileLocation> results = service.searchFile("movie.mp4");
        
        // System.out.println("Search results:");
        // for (FileLocation f : results) {
        //     System.out.println(f.clientId + " " + f.host + ":" + f.port);
        // }
        
        System.out.println(service.getClients());
        while (true) {
            service.heartbeat(clientId);
            System.out.println("Heartbeat sent for " + clientId);
            Thread.sleep(5000); // send heartbeat every 5 seconds
        }
    }
}