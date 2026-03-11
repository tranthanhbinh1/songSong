package com.dd.daemon;

public record DaemonConfig(
        String clientId,
        String host,
        int port,
        int peerPort,
        String directoryHost,
        int directoryPort,
        long heartbeatInterval,
        long shareDir) {
    public static DaemonConfig fromArgs(String[] args) {
        if (args.length != 8) {
            throw new IllegalArgumentException(
                    "Usage: <clientId> <host> <peerPort> <sharedDir> [directoryHost=localhost] [directoryPort=1099] [heartbeatMs=5000]");
        }
        return new DaemonConfig(
                args[0],
                args[1],
                Integer.parseInt(args[2]),
                Integer.parseInt(args[3]),
                args[4],
                Integer.parseInt(args[5]),
                Long.parseLong(args[6]),
                Long.parseLong(args[7]));
    }
}
