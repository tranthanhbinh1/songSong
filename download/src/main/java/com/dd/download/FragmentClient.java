package com.dd.download;

import com.dd.shared.FileLocation;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.zip.GZIPInputStream;

public class FragmentClient {
    public byte[] fetchFragment(
            FileLocation location,
            String filename,
            long offset,
            int length,
            boolean compressionEnabled)
            throws IOException {
        try (Socket socket = new Socket(location.host, location.port);
                DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
            output.writeUTF("GET_FRAGMENT");
            output.writeUTF(filename);
            output.writeLong(offset);
            output.writeInt(length);
            output.writeBoolean(compressionEnabled);
            output.flush();

            byte status = input.readByte();
            if (status != 0) {
                throw new IOException("Fragment request failed with status " + status + " from " + location.clientId);
            }

            int originalLength = input.readInt();
            int payloadLength = input.readInt();
            byte[] payload = input.readNBytes(payloadLength);
            byte[] bytes = compressionEnabled ? inflate(payload) : payload;
            if (bytes.length != originalLength) {
                throw new IOException("Expected " + originalLength + " bytes after decompression but received "
                        + bytes.length);
            }
            return bytes;
        }
    }

    private byte[] inflate(byte[] compressedBytes) throws IOException {
        try (GZIPInputStream gzipInput = new GZIPInputStream(new ByteArrayInputStream(compressedBytes))) {
            return gzipInput.readAllBytes();
        }
    }
}
