package com.dd.shared;

import java.io.Serializable;

public class FileLocation implements Serializable {

    public String clientId;
    public String host;
    public int port;

    public FileLocation(String clientId, String host, int port) {
        this.clientId = clientId;
        this.host = host;
        this.port = port;
    }
}