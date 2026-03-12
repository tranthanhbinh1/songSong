package com.dd.shared;

import java.io.Serializable;

public class FileLocation implements Serializable {

    public String clientId;
    public String host;
    public int port;
    public long size;

    public FileLocation(String clientId, String host, int port, long size) {
        this.clientId = clientId;
        this.host = host;
        this.port = port;
        this.size = size;
    }
}
