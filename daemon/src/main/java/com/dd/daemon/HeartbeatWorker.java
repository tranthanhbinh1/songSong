package com.dd.daemon;
import java.util.concurrent.*;

public class HeartbeatWorker implements AutoCloseable{
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void start(DirectoryClient directoryClient, String clientId, long interval) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                directoryClient.heartbeat(clientId);
                System.out.println("Heartbeat sent for " + clientId);
            } catch (Exception e) {
                System.err.println("Failed to send heartbeat: " + e.getMessage());
            }
        }, 0, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
