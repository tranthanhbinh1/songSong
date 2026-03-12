package com.dd.download;

public record DownloadConfig(
        String filename,
        String directoryHost,
        int directoryPort,
        String outputDir,
        int chunkSizeBytes) {

    public static DownloadConfig fromArgs(String[] args) {
        if (args.length < 1 || args.length > 5) {
            throw new IllegalArgumentException(
                    "Usage: <filename> [directoryHost=localhost] [directoryPort=1099] [outputDir=downloads] [chunkSizeBytes=1048576]");
        }

        int chunkSizeBytes = args.length >= 5 ? Integer.parseInt(args[4]) : 1_048_576;
        if (chunkSizeBytes <= 0) {
            throw new IllegalArgumentException("chunkSizeBytes must be greater than zero.");
        }

        return new DownloadConfig(
                args[0],
                args.length >= 2 ? args[1] : "localhost",
                args.length >= 3 ? Integer.parseInt(args[2]) : 1099,
                args.length >= 4 ? args[3] : "downloads",
                chunkSizeBytes);
    }
}
