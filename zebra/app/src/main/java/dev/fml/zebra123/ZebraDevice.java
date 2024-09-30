package dev.fml.zebra123;

import java.util.ArrayList;
import java.util.HashMap;

public interface ZebraDevice {

    public void connect();
    public void disconnect();
    public void dispose();
    public void setMode(String mode);
    public void scan(ZebraScanRequest request);
    public void track(ZebraScanRequest request, ArrayList<String> tags);
    public void write(String epc, String newEpc, String password, String newPassword, String data);

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
        error,
        connectionStatus,
        startRead,
        stopRead,
        writeFail,
        writeSuccess,
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