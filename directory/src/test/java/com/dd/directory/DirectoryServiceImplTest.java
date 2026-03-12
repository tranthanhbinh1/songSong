package com.dd.directory;

import com.dd.shared.FileLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectoryServiceImplTest {
    private DirectoryServiceImpl service;

    @AfterEach
    void tearDown() throws Exception {
        if (service != null) {
            UnicastRemoteObject.unexportObject(service, true);
        }
    }

    @Test
    void returnsAllProvidersWhenSizesMatch() throws Exception {
        service = new DirectoryServiceImpl(5_000L, 1_000L);
        service.registerClient("client1", "localhost", 5000);
        service.registerClient("client2", "localhost", 5001);
        service.registerFile("client1", "movie.mp4", 1024L);
        service.registerFile("client2", "movie.mp4", 1024L);

        List<FileLocation> results = service.searchFile("movie.mp4");

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(location -> location.size == 1024L));
    }

    @Test
    void excludesProviderWhenFileSizeDoesNotMatch() throws Exception {
        service = new DirectoryServiceImpl(5_000L, 1_000L);
        service.registerClient("client1", "localhost", 5000);
        service.registerClient("client2", "localhost", 5001);
        service.registerFile("client1", "movie.mp4", 1024L);
        service.registerFile("client2", "movie.mp4", 2048L);

        List<FileLocation> results = service.searchFile("movie.mp4");

        assertEquals(1, results.size());
        assertEquals("client1", results.get(0).clientId);
        assertEquals(1024L, results.get(0).size);
    }

    @Test
    void removesExpiredClientsFromSearchResults() throws Exception {
        service = new DirectoryServiceImpl(100L, 25L);
        service.registerClient("client1", "localhost", 5000);
        service.registerFile("client1", "movie.mp4", 1024L);

        waitUntilExpired();

        assertTrue(service.searchFile("movie.mp4").isEmpty());
    }

    private void waitUntilExpired() throws Exception {
        long deadline = System.currentTimeMillis() + 2_000L;
        while (System.currentTimeMillis() < deadline) {
            if (service.searchFile("movie.mp4").isEmpty()) {
                return;
            }
            Thread.sleep(25L);
        }
    }
}
