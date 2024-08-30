package dev.fml.zebra123;

import java.util.HashMap;

public interface ZebraDeviceListener {
    public void notify(final ZebraDevice.ZebraInterfaces source, final ZebraDevice.ZebraEvents event, final HashMap map);
}