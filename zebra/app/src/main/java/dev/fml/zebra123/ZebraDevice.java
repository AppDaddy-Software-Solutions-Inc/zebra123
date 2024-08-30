package dev.fml.zebra123;

import java.util.HashMap;

public interface ZebraDevice {

    public void connect();
    public void disconnect();
    public void setMode(String mode);

    enum ZebraInterfaces {
        zebraSdk,
        dataWedge,
        unknown
    }

    enum ZebraEvents {
        readRfid,
        readBarcode,
        error,
        connectionStatus,
        unknown
    }

    enum ZebraConnectionStatus {
        disconnected,
        connected,
        error,
        unknown
    }

    public static HashMap<String, Object> toError(String source, Exception exception) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("source", source);
        map.put("message", exception.getMessage());
        map.put("trace", exception.getStackTrace().toString());
        return map;
    }
}