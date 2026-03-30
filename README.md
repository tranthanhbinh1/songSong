# SongSong

Parallel file download prototype built with Java 17, Java RMI, and TCP sockets.

The project has three components:
- `directory`: central RMI registry of clients and files
- `daemon`: runs on each provider, registers local files, sends heartbeats, and serves file fragments over TCP
- `download`: looks up providers from the directory and downloads file fragments in parallel

## Prerequisites

- Java 17
- A terminal opened at the project root

```bash
cd /Users/.../.../songSong
```

If `./gradlew` is not executable on your machine, use `bash ./gradlew`.

## Build

```bash
bash ./gradlew build
```

This compiles the project, runs tests, and builds the jars.

## Generate Launch Scripts

To run the modules directly without Gradle, generate the application launchers:

```bash
bash ./gradlew :directory:installDist :daemon:installDist :download:installDist
```

This creates runnable scripts under each module's `build/install/.../bin/` directory.

## Local Demo Setup

Create two local provider folders and one output folder:

```bash
mkdir -p /tmp/songSong/provider1
mkdir -p /tmp/songSong/provider2
mkdir -p /tmp/songSong/output
```

Copy the same file into both provider folders:

```bash
cp /path/to/your/file.bin /tmp/songSong/provider1/
cp /path/to/your/file.bin /tmp/songSong/provider2/
```

Important notes:
- The filename must be exactly the same on all providers.
- File identity is based on filename only.
- Files are scanned only when the daemon starts. If you add or remove files later, restart that daemon.
- File fragments can be transferred either raw or gzip-compressed; shared files remain unchanged on disk.

## Run the Directory

Open a first terminal:

```bash
cd /Users/.../.../songSong
directory/build/install/directory/bin/directory localhost 1099 1100
```

Expected log:

```text
Directory RMI server running on localhost (registry port 1099, service port 1100)
```

Directory arguments:

```text
[advertisedHost=localhost] [registryPort=1099] [servicePort=1100]
```

The directory now uses:
- an RMI registry port, default `1099`
- a fixed RMI service port, default `1100`
- an advertised hostname, which must be reachable by all daemons and download clients

## Run the Daemons

Open a second terminal for daemon 1:

```bash
cd /Users/.../.../songSong
daemon/build/install/daemon/bin/daemon client1 127.0.0.1 5001 /tmp/songSong/provider1 127.0.0.1 1099
```

Open a third terminal for daemon 2:

```bash
cd /Users/.../.../songSong
daemon/build/install/daemon/bin/daemon client2 127.0.0.1 5002 /tmp/songSong/provider2 127.0.0.1 1099
```

Daemon arguments:

```text
<clientId> <host> <peerPort> <sharedDir> <directoryHost> <directoryPort> [heartbeatMs]
```

Argument meaning:
- `clientId`: unique daemon identifier
- `host`: host advertised to download clients
- `peerPort`: TCP port used to serve fragments
- `sharedDir`: local folder to scan and share
- `directoryHost`, `directoryPort`: where the directory server is running
- `heartbeatMs`: optional heartbeat interval, default `5000`

For a local demo, `127.0.0.1` is fine.

For a multi-machine run over Tailscale, use the daemon machine's Tailscale DNS name or Tailscale IP as the `host`
argument, and use the directory machine's Tailscale DNS name or Tailscale IP as `directoryHost`.

Expected daemon logs include:
- client registration
- file registration
- `Fragment server listening on port ...`
- periodic heartbeat messages

## Run a Download

Open a fourth terminal:

```bash
cd /Users/.../.../songSong
download/build/install/download/bin/download file.bin 127.0.0.1 1099 /tmp/songSong/output 262144
```

Download arguments:

```text
<filename> [directoryHost=localhost] [directoryPort=1099] [outputDir=downloads] [chunkSizeBytes=1048576] [compressionEnabled=true]
```

Example meaning:
- download `file.bin`
- contact the directory at `127.0.0.1:1099`
- store the final file in `/tmp/songSong/output`
- use `262144` bytes per fragment
- enable gzip compression for fragment transfer

If the download succeeds, the result will be:

```bash
/tmp/songSong/output/file.bin
```

Compression note:
- Compression is controlled by the optional last download argument.
- `true` means fragments are gzip-compressed in transit.
- `false` means fragments are transferred raw, which is useful for benchmarking.
- The shared files on disk remain unchanged in both modes.

## Verify the Result

Compare checksums between a provider copy and the downloaded file:

```bash
shasum /tmp/songSong/provider1/file.bin
shasum /tmp/songSong/output/file.bin
```

The hashes should match.
Compression, when enabled, happens only in transit, so there is no cache directory or compressed copy created in the shared folder.

## Run Across Two Machines With Tailscale

On the machine running the directory:

```bash
cd /Users/.../.../songSong
directory/build/install/directory/bin/directory binhs-macbook-pro.tailae9542.ts.net 1099 1100
```

On the second machine running the daemon:

```bash
cd /Users/.../.../songSong
daemon/build/install/daemon/bin/daemon client1 tb24-workstation.tailae9542.ts.net 5001 /tmp/songSong/provider1 binhs-macbook-pro.tailae9542.ts.net 1099
```

If you download from either machine, point the downloader at the directory host:

```bash
cd /Users/.../.../songSong
download/build/install/download/bin/download file.bin binhs-macbook-pro.tailae9542.ts.net 1099 /tmp/songSong/output 262144
```

## Test the Compression Feature

### Automated tests

Run the full test suite:

```bash
bash ./gradlew test
```

Compression-specific coverage is included in:
- `daemon/src/test/java/com/dd/daemon/FragmentServerTest.java`
- `download/src/test/java/com/dd/download/FragmentClientTest.java`
- `download/src/test/java/com/dd/download/ParallelDownloaderIntegrationTest.java`

These tests verify that:
- the daemon sends gzip-compressed fragment payloads
- the download client correctly decompresses them
- parallel download still reconstructs the original file correctly

### Benchmark compressed vs uncompressed

Run without compression:

```bash
time download/build/install/download/bin/download bigfile.bin 127.0.0.1 1099 /tmp/songSong/output 262144 false
```

Run with compression:

```bash
time download/build/install/download/bin/download bigfile.bin 127.0.0.1 1099 /tmp/songSong/output 262144 true
```

Compare the `total` time from both commands. Use the same file, same chunk size, and the same running daemons for a fair comparison.

## Run Without Gradle

After running both `build` and `installDist`, start the generated launch scripts directly:

```bash
directory/build/install/directory/bin/directory
daemon/build/install/daemon/bin/daemon <args...>
download/build/install/download/bin/download <args...>
```

This avoids Gradle startup overhead and is the recommended way to demo and benchmark the project.

## Run Tests

```bash
bash ./gradlew test
```

Or run the full build:

```bash
bash ./gradlew build
```
