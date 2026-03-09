package com.dd.shared;

import java.io.Serializable;

public class ClientInfo implements Serializable {

    public String clientId;
    public String host;
    public int port;
    public long lastHeartbeat;

    public ClientInfo(String clientId, String host, int port) {
        this.clientId = clientId;
        this.host = host;
        this.port = port;
        this.lastHeartbeat = System.currentTimeMillis();
    }
}