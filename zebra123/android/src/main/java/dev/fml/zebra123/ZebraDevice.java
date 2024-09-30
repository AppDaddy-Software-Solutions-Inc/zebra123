package dev.fml.zebra123;

import java.util.ArrayList;
import java.util.HashMap;

public interface ZebraDevice {

    void connect();
    void disconnect();
    void dispose();
    void scan(ZebraScanRequest request);
    void track(ZebraScanRequest request, ArrayList<String> tags);
    void write(String epc, String newEpc, String password, String newPassword, String data);

    enum ZebraInterfaces {
        rfidapi3,
        datawedge,
        unknown
    }


    enum ZebraScanRequest {
        rfidStartScanning,
        rfidStopScanning,
        rfidStartTracking,
        rfidStopTracking,
        unknown
    }

    enum ZebraEvents {
        readRfid,
        readBarcode,
        startRead,
        stopRead,
        writeFail,
        writeSuccess,
        error,
        connectionStatus,
        support,
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