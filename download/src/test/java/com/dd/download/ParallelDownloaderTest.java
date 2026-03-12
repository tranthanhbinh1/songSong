package com.dd.download;

import com.dd.daemon.FragmentServer;
import com.dd.directory.DirectoryServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ParallelDownloaderTest {
    @TempDir
    Path tempDir;

    private int registryPort;
    private DirectoryServiceImpl service;
    private Registry registry;
    private FragmentServer server1;
    private FragmentServer server2;

    @AfterEach
    void tearDown() throws Exception {
        if (server1 != null) {
            server1.close();
        }
        if (server2 != null) {
            server2.close();
        }
        if (service != null) {
            UnicastRemoteObject.unexportObject(service, true);
        }
        if (registry != null) {
            UnicastRemoteObject.unexportObject(registry, true);
        }
    }

    @Test
    void downloadsFileFromMultipleProviders() throws Exception {
        Path shared1 = tempDir.resolve("provider1.bin");
        Path shared2 = tempDir.resolve("provider2.bin");
        byte[] content = createContent(512);
        Files.write(shared1, content);
        Files.write(shared2, content);

        startDirectory();
        server1 = startServer(shared1, "payload.bin");
        server2 = startServer(shared2, "payload.bin");
        registerProvider("client1", server1.getPort(), "payload.bin", Files.size(shared1));
        registerProvider("client2", server2.getPort(), "payload.bin", Files.size(shared2));

        ParallelDownloader downloader = new ParallelDownloader(
                new DirectoryClient("localhost", registryPort),
                new FragmentClient());
        Path output = downloader.download(new DownloadConfig("payload.bin", "localhost", registryPort,
                tempDir.resolve("downloads").toString(), 64));

        assertEquals("payload.bin", output.getFileName().toString());
        assertArrayEquals(content, Files.readAllBytes(output));
    }

    @Test
    void downloadsFileFromSingleProvider() throws Exception {
        Path shared = tempDir.resolve("single.bin");
        byte[] content = createContent(128);
        Files.write(shared, content);

        startDirectory();
        server1 = startServer(shared, "single.bin");
        registerProvider("client1", server1.getPort(), "single.bin", Files.size(shared));

        ParallelDownloader downloader = new ParallelDownloader(
                new DirectoryClient("localhost", registryPort),
                new FragmentClient());
        Path output = downloader.download(new DownloadConfig("single.bin", "localhost", registryPort,
                tempDir.resolve("downloads-single").toString(), 32));

        assertArrayEquals(content, Files.readAllBytes(output));
    }

    private void startDirectory() throws Exception {
        service = new DirectoryServiceImpl(5_000L, 1_000L);
        registryPort = findFreePort();
        registry = LocateRegistry.createRegistry(registryPort);
        registry.rebind("DirectoryService", service);
    }

    private FragmentServer startServer(Path file, String sharedName) throws Exception {
        FragmentServer server = new FragmentServer(0, Map.of(sharedName, file));
        server.start();
        return server;
    }

    private void registerProvider(String clientId, int port, String filename, long size) throws Exception {
        service.registerClient(clientId, "localhost", port);
        service.registerFile(clientId, filename, size);
    }

    private int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private byte[] createContent(int size) {
        byte[] content = new byte[size];
        for (int i = 0; i < size; i++) {
            content[i] = (byte) (i % 251);
        }
        return content;
    }
}
