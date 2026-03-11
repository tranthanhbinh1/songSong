package com.dd.daemon;

public record DaemonConfig(
        String clientId,
        String host,
        int peerPort,
        String shareDir,
        String directoryHost,
        int directoryPort,
        long heartbeatInterval) {
    public static DaemonConfig fromArgs(String[] args) {
        if (args.length != 6 && args.length != 7) {
            throw new IllegalArgumentException(
                    "Usage: <clientId> <host> <peerPort> <sharedDir> <directoryHost> <directoryPort> [heartbeatMs=5000]");
        }
        return new DaemonConfig(
                args[0],
                args[1],
                Integer.parseInt(args[2]),
                args[3],
                args[4],
                Integer.parseInt(args[5]),
                args.length == 7 ? Long.parseLong(args[6]) : 5000L);
    }
}
