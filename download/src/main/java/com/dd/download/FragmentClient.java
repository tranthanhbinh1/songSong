package com.dd.download;

import com.dd.shared.FileLocation;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class FragmentClient {
    public byte[] fetchFragment(FileLocation location, String filename, long offset, int length) throws IOException {
        try (Socket socket = new Socket(location.host, location.port);
                DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
            output.writeUTF("GET_FRAGMENT");
            output.writeUTF(filename);
            output.writeLong(offset);
            output.writeInt(length);
            output.flush();

            byte status = input.readByte();
            if (status != 0) {
                throw new IOException("Fragment request failed with status " + status + " from " + location.clientId);
            }

            int actualLength = input.readInt();
            return input.readNBytes(actualLength);
        }
    }
}
