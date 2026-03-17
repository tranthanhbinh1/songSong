package com.dd.directory;

import com.dd.shared.DirectoryService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class DirectoryServer {

    public static void main(String[] args) throws Exception {
        DirectoryServerConfig config = DirectoryServerConfig.fromArgs(args);
        System.setProperty("java.rmi.server.hostname", config.advertisedHost());

        DirectoryService service = new DirectoryServiceImpl(config.servicePort());

        Registry registry = LocateRegistry.createRegistry(config.registryPort());

        registry.rebind("DirectoryService", service);

        System.out.println("Directory RMI server running on " + config.advertisedHost()
                + " (registry port " + config.registryPort()
                + ", service port " + config.servicePort() + ")");
    }
}
