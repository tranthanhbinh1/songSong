package com.dd.download;

import java.nio.file.Path;

public class DownloadApp {
    public static void main(String[] args) throws Exception {
        DownloadConfig config = DownloadConfig.fromArgs(args);
        ParallelDownloader downloader = new ParallelDownloader(
                new DirectoryClient(config.directoryHost(), config.directoryPort()),
                new FragmentClient());

        Path downloadedFile = downloader.download(config);
        System.out.println("Downloaded file to " + downloadedFile);
    }
}
