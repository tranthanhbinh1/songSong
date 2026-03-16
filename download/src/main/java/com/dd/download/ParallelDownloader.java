package com.dd.download;

import com.dd.shared.FileLocation;

import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ParallelDownloader {
    private final DirectoryClient directoryClient;
    private final FragmentClient fragmentClient;

    public ParallelDownloader(DirectoryClient directoryClient, FragmentClient fragmentClient) {
        this.directoryClient = directoryClient;
        this.fragmentClient = fragmentClient;
    }

    public Path download(DownloadConfig config) throws Exception {
        List<FileLocation> providers = directoryClient.searchFile(config.filename());
        if (providers.isEmpty()) {
            throw new IOException("No providers found for file " + config.filename());
        }

        long fileSize = providers.get(0).size;
        Path outputDirectory = Path.of(config.outputDir()).toAbsolutePath().normalize();
        Files.createDirectories(outputDirectory);

        Path finalPath = outputDirectory.resolve(config.filename());
        if (fileSize == 0) {
            Files.deleteIfExists(finalPath);
            return Files.write(finalPath, new byte[0]);
        }

        Path partPath = outputDirectory.resolve(config.filename() + ".part");
        Files.deleteIfExists(partPath);

        int chunkCount = (int) ((fileSize + config.chunkSizeBytes() - 1) / config.chunkSizeBytes());
        int workerCount = Math.max(1, Math.min(providers.size(), chunkCount));
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(partPath.toFile(), "rw");
                FileChannel outputChannel = randomAccessFile.getChannel()) {
            randomAccessFile.setLength(fileSize);
            List<Future<Void>> futures = new ArrayList<>();
            for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
                ChunkAssignment assignment = createAssignment(config, providers, fileSize, chunkIndex);
                futures.add(executor.submit(downloadChunk(assignment, outputChannel)));
            }

            for (Future<Void> future : futures) {
                future.get();
            }
        } catch (ExecutionException e) {
            executor.shutdownNow();
            Files.deleteIfExists(partPath);
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new IOException("Download failed for " + config.filename() + ": " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Files.deleteIfExists(partPath);
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted for " + config.filename(), e);
        } catch (IOException e) {
            executor.shutdownNow();
            Files.deleteIfExists(partPath);
            throw e;
        } finally {
            executor.shutdownNow();
        }

        Files.deleteIfExists(finalPath);
        return Files.move(partPath, finalPath);
    }

    private Callable<Void> downloadChunk(ChunkAssignment assignment, FileChannel outputChannel) {
        return () -> {
            byte[] bytes = fragmentClient.fetchFragment(
                    assignment.provider(),
                    assignment.filename(),
                    assignment.offset(),
                    assignment.length(),
                    assignment.compressionEnabled());
            if (bytes.length != assignment.length()) {
                throw new IOException("Expected " + assignment.length() + " bytes but received " + bytes.length);
            }
            writeAll(outputChannel, assignment.offset(), bytes);
            return null;
        };
    }

    private ChunkAssignment createAssignment(
            DownloadConfig config,
            List<FileLocation> providers,
            long fileSize,
            int chunkIndex) {
        long offset = (long) chunkIndex * config.chunkSizeBytes();
        int length = (int) Math.min(config.chunkSizeBytes(), fileSize - offset);
        FileLocation provider = providers.get(chunkIndex % providers.size());
        return new ChunkAssignment(provider, config.filename(), offset, length, config.compressionEnabled());
    }

    private void writeAll(FileChannel outputChannel, long offset, byte[] bytes) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long position = offset;
        while (buffer.hasRemaining()) {
            position += outputChannel.write(buffer, position);
        }
    }

    private record ChunkAssignment(
            FileLocation provider,
            String filename,
            long offset,
            int length,
            boolean compressionEnabled) {
    }
}
