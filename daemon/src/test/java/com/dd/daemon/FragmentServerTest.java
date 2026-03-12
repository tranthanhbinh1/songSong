package com.dd.daemon;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FragmentServerTest {
    @TempDir
    Path tempDir;

    private FragmentServer server;

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void servesFragmentsFromStartMiddleAndEnd() throws Exception {
        Path file = tempDir.resolve("sample.bin");
        byte[] content = "abcdefghijklmnopqrstuvwxyz".getBytes();
        Files.write(file, content);

        server = new FragmentServer(0, Map.of(file.getFileName().toString(), file));
        server.start();

        FragmentResponse start = request("sample.bin", 0, 5);
        FragmentResponse middle = request("sample.bin", 10, 4);
        FragmentResponse end = request("sample.bin", 23, 3);

        assertEquals(FragmentServer.STATUS_OK, start.status());
        assertArrayEquals("abcde".getBytes(), start.bytes());
        assertArrayEquals("klmn".getBytes(), middle.bytes());
        assertArrayEquals("xyz".getBytes(), end.bytes());
    }

    @Test
    void returnsStatusCodesForMissingFilesAndInvalidRanges() throws Exception {
        Path file = tempDir.resolve("sample.bin");
        Files.write(file, "abcdef".getBytes());

        server = new FragmentServer(0, Map.of(file.getFileName().toString(), file));
        server.start();

        FragmentResponse missing = request("missing.bin", 0, 3);
        FragmentResponse invalid = request("sample.bin", 99, 3);

        assertEquals(FragmentServer.STATUS_FILE_NOT_FOUND, missing.status());
        assertEquals(FragmentServer.STATUS_INVALID_RANGE, invalid.status());
    }

    @Test
    void handlesConcurrentRequestsAgainstSameFile() throws Exception {
        Path file = tempDir.resolve("sample.bin");
        byte[] content = new byte[512];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) i;
        }
        Files.write(file, content);

        server = new FragmentServer(0, Map.of(file.getFileName().toString(), file));
        server.start();

        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<FragmentResponse>> calls = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                long offset = i * 32L;
                calls.add(() -> request("sample.bin", offset, 32));
            }

            List<Future<FragmentResponse>> futures = executor.invokeAll(calls);
            for (int i = 0; i < futures.size(); i++) {
                FragmentResponse response = futures.get(i).get();
                assertEquals(FragmentServer.STATUS_OK, response.status());
                assertEquals(32, response.bytes().length);
                assertTrue(response.bytes()[0] == (byte) (i * 32));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private FragmentResponse request(String filename, long offset, int length) throws Exception {
        try (Socket socket = new Socket("localhost", server.getPort());
                DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
            output.writeUTF("GET_FRAGMENT");
            output.writeUTF(filename);
            output.writeLong(offset);
            output.writeInt(length);
            output.flush();

            byte status = input.readByte();
            if (status != FragmentServer.STATUS_OK) {
                return new FragmentResponse(status, new byte[0]);
            }

            int actualLength = input.readInt();
            return new FragmentResponse(status, input.readNBytes(actualLength));
        }
    }

    private record FragmentResponse(byte status, byte[] bytes) {
    }
}
