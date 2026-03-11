package com.dd.daemon;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class DaemonMain {
    public static void main(String[] args) throws Exception {
        DaemonConfig config = DaemonConfig.fromArgs(args);
        DirectoryClient directoryClient = new DirectoryClient(config.directoryHost(), config.directoryPort());

        // Register client
        directoryClient.registerClient(config.clientId(), config.host(), config.peerPort());
        System.out.println("Registered client " + config.clientId() + " with directory service.");

        // Scan and register files
        LocalFileCatalog catalog = new LocalFileCatalog(config.shareDir());
        Map<String, Path> files = catalog.scan();

        for (Path p : files.values()) {
            long size = Files.size(p);
            directoryClient.registerFile(config.clientId(), p, size);
            System.out
                    .println("Registered file " + p.getFileName() + " with size " + size + " with directory service.");
        }

        try (HeartbeatWorker heartbeatWorker = new HeartbeatWorker()) {
            heartbeatWorker.start(directoryClient, config.clientId(), config.heartbeatInterval());
            Thread.currentThread().join();
        }
    }
}
