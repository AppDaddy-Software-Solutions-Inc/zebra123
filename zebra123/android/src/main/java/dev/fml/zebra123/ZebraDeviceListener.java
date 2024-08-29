package dev.fml.zebra123;

import java.util.HashMap;

public interface ZebraDeviceListener {

    public void notify(final String event, final HashMap map);
    public void notify(final String event, Exception exception);
}