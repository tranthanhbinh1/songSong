package com.dd.daemon;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentServer implements AutoCloseable {
    static final byte STATUS_OK = 0;
    static final byte STATUS_FILE_NOT_FOUND = 1;
    static final byte STATUS_INVALID_RANGE = 2;
    static final byte STATUS_SERVER_ERROR = 3;

    private final Map<String, Path> filesByName;
    private final ServerSocket serverSocket;
    private final ExecutorService requestExecutor;
    private Thread acceptThread;

    public FragmentServer(int port, Map<String, Path> filesByName) throws IOException {
        this.filesByName = filesByName;
        this.serverSocket = new ServerSocket(port);
        this.requestExecutor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors()));
    }

    public void start() {
        acceptThread = new Thread(this::acceptLoop, "fragment-server-" + serverSocket.getLocalPort());
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                requestExecutor.execute(() -> handleClient(socket));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("Fragment server accept failed: " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        try (Socket clientSocket = socket;
                DataInputStream input = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                DataOutputStream output = new DataOutputStream(
                        new BufferedOutputStream(clientSocket.getOutputStream()))) {
            String command = input.readUTF();
            if (!"GET_FRAGMENT".equals(command)) {
                output.writeByte(STATUS_SERVER_ERROR);
                output.flush();
                return;
            }

            String filename = input.readUTF();
            long offset = input.readLong();
            int length = input.readInt();
            serveFragment(filename, offset, length, output);
        } catch (IOException e) {
            System.err.println("Fragment request failed: " + e.getMessage());
        }
    }

    private void serveFragment(String filename, long offset, int length, DataOutputStream output) throws IOException {
        Path filePath = filesByName.get(filename);
        if (filePath == null || !Files.isRegularFile(filePath)) {
            output.writeByte(STATUS_FILE_NOT_FOUND);
            output.flush();
            return;
        }

        long fileSize = Files.size(filePath);
        if (offset < 0 || offset >= fileSize || length <= 0) {
            output.writeByte(STATUS_INVALID_RANGE);
            output.flush();
            return;
        }

        int actualLength = (int) Math.min((long) length, fileSize - offset);
        if (actualLength < 0) {
            output.writeByte(STATUS_INVALID_RANGE);
            output.flush();
            return;
        }

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(filePath.toFile(), "r")) {
            randomAccessFile.seek(offset); // Set the offset for the fragment before reading
            byte[] buffer = new byte[actualLength];
            randomAccessFile.readFully(buffer);

            output.writeByte(STATUS_OK);
            output.writeInt(actualLength);
            output.write(buffer);
            output.flush();
        } catch (IOException e) {
            output.writeByte(STATUS_SERVER_ERROR);
            output.flush();
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
        requestExecutor.shutdownNow();
    }
}
