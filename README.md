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
cd /Users/binhtran/projects/usth-sys-arch/songSong
```

If `./gradlew` is not executable on your machine, use `bash ./gradlew`.

## Build

```bash
bash ./gradlew build
```

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
- File fragments are gzip-compressed transparently during transfer; shared files remain unchanged on disk.

## Run the Directory

Open a first terminal:

```bash
cd /Users/binhtran/projects/usth-sys-arch/songSong
bash ./gradlew :directory:run
```

Expected log:

```text
Directory RMI server running...
```

The directory currently listens on fixed port `1099`.

## Run the Daemons

Open a second terminal for daemon 1:

```bash
cd /Users/binhtran/projects/usth-sys-arch/songSong
bash ./gradlew :daemon:run --args='client1 127.0.0.1 5001 /tmp/songSong/provider1 127.0.0.1 1099'
```

Open a third terminal for daemon 2:

```bash
cd /Users/binhtran/projects/usth-sys-arch/songSong
bash ./gradlew :daemon:run --args='client2 127.0.0.1 5002 /tmp/songSong/provider2 127.0.0.1 1099'
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

Expected daemon logs include:
- client registration
- file registration
- `Fragment server listening on port ...`
- periodic heartbeat messages

## Run a Download

Open a fourth terminal:

```bash
cd /Users/binhtran/projects/usth-sys-arch/songSong
bash ./gradlew :download:run --args='file.bin 127.0.0.1 1099 /tmp/songSong/output 262144'
```

Download arguments:

```text
<filename> [directoryHost=localhost] [directoryPort=1099] [outputDir=downloads] [chunkSizeBytes=1048576]
```

Example meaning:
- download `file.bin`
- contact the directory at `127.0.0.1:1099`
- store the final file in `/tmp/songSong/output`
- use `262144` bytes per fragment

If the download succeeds, the result will be:

```bash
/tmp/songSong/output/file.bin
```

Compression note:
- There is no extra flag to enable compression.
- Every `GET_FRAGMENT` response is gzip-compressed automatically by the daemon.
- The downloader decompresses each fragment before writing the final file.

## Verify the Result

Compare checksums between a provider copy and the downloaded file:

```bash
shasum /tmp/songSong/provider1/file.bin
shasum /tmp/songSong/output/file.bin
```

The hashes should match.
Fragments are compressed only in transit, so there is no cache directory or compressed copy created in the shared folder.

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

### Manual local check

Use a highly compressible file so the feature is easier to reason about:

```bash
python3 - <<'PY'
from pathlib import Path
data = ("songSong compression demo\n" * 200000).encode()
Path("/tmp/songSong/provider1/compressible.txt").write_bytes(data)
Path("/tmp/songSong/provider2/compressible.txt").write_bytes(data)
PY
```

Then run the directory, both daemons, and the downloader exactly as shown above, but request:

```bash
bash ./gradlew :download:run --args='compressible.txt 127.0.0.1 1099 /tmp/songSong/output 262144'
```

Finally, verify the downloaded file:

```bash
shasum /tmp/songSong/provider1/compressible.txt
shasum /tmp/songSong/output/compressible.txt
```

The hashes should match. That confirms the file survived the gzip-compress/decompress transfer path correctly.

## Use Generated Launch Scripts

If you prefer launch scripts instead of `gradlew :module:run`, generate them with:

```bash
bash ./gradlew :directory:installDist :daemon:installDist :download:installDist
```

Then run:

```bash
directory/build/install/directory/bin/directory
daemon/build/install/daemon/bin/daemon
download/build/install/download/bin/download
```

The daemon and download arguments remain the same.

## Run Tests

```bash
bash ./gradlew test
```

Or run the full build:

```bash
bash ./gradlew build
```

## Common Issues

- `No providers found for file ...`
  - The requested filename does not match the shared filename.
  - The daemon did not register the file.
  - The directory is not running.

- `Connection refused`
  - The daemon port is wrong.
  - The daemon host is wrong.
  - The daemon is not running.

- Newly added files do not appear
  - Restart the daemon because it only scans files at startup.

- Download fails when a provider disconnects
  - This is expected in the current version. Retry/resume is not implemented yet.

- Port already in use
  - Change daemon ports such as `5001` and `5002`.
  - The directory port is currently fixed at `1099`.

## Current Limitations

- No retry or fragment reassignment when a provider fails
- No dynamic discovery of new providers during an active download
- No load-aware provider selection
- The directory port is hardcoded to `1099`

## Minimal End-to-End Sequence

1. Build the project with `bash ./gradlew build`.
2. Start the directory.
3. Start two daemons with the same file in separate folders.
4. Run the downloader for that filename.
5. Verify the downloaded file checksum.
