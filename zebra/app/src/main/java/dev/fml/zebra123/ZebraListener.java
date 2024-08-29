package dev.fml.zebra123;

import java.util.HashMap;

public interface ZebraListener {

    public void onZebraListenerSuccess(final String event, final HashMap map);

    public void onZebraListenerError(final String event, final HashMap map);
}
