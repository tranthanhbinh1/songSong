package com.dd.daemon;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class LocalFileCatalog {
    private final Path shareDir;
    private final Map<String, Path> mapByName = new HashMap<>();

    public LocalFileCatalog(String shareDir) {
        this.shareDir = Paths.get(shareDir).toAbsolutePath().normalize();
    }

    public Map<String, Path> scan() throws IOException {
        mapByName.clear();
        if (!Files.isDirectory(shareDir))
            return Map.of();
        try (Stream<Path> paths = Files.walk(shareDir)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                String fileName = path.getFileName().toString();
                mapByName.put(fileName, path);
            });
        }
        return Map.copyOf(mapByName);
    }
}
