package dev.fml.zebra123;

import java.util.ArrayList;
import java.util.HashMap;

public interface ZebraDevice {

    void connect();
    void disconnect();
    void dispose();
    void scan(Requests request);
    void track(Requests request, ArrayList<String> tags);
    void write(String epc, String newEpc, String password, String newPassword, String data);
    void setMode(Modes mode);

    enum Interfaces {
        rfidapi3,
        datawedge,
        unknown
    }

    enum Methods {
        track,
        scan,
        write,
        mode,
        unknown
    }

    enum Requests {
        start,
        stop,
        unknown
    }

    enum Modes {
        barcode,
        rfid,
        mixed
    }

    enum Events {
        readRfid,
        readBarcode,
        error,
        connectionStatus,
        support,
        startRead,
        stopRead,
        writeFail,
        writeSuccess,
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