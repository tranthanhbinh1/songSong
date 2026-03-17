package com.dd.directory;

public record DirectoryServerConfig(
        String advertisedHost,
        int registryPort,
        int servicePort) {
    private static final String USAGE =
            "Usage: [advertisedHost=localhost] [registryPort=1099] [servicePort=1100]";

    public static DirectoryServerConfig fromArgs(String[] args) {
        if (args.length > 3) {
            throw new IllegalArgumentException(USAGE);
        }

        int registryPort = args.length >= 2 ? parsePort(args[1], "registryPort") : 1099;
        int servicePort = args.length >= 3 ? parsePort(args[2], "servicePort") : 1100;

        return new DirectoryServerConfig(
                args.length >= 1 ? args[0] : "localhost",
                registryPort,
                servicePort);
    }

    private static int parsePort(String value, String fieldName) {
        int port = Integer.parseInt(value);
        if (port <= 0 || port > 65_535) {
            throw new IllegalArgumentException(fieldName + " must be between 1 and 65535.");
        }
        return port;
    }
}
