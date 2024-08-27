package dev.fml.zebra123;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.zebra.rfid.api3.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class ZebraWrapper extends Zebra {

    private EventChannel.EventSink sink = null;
    public Handler handler = new Handler(Looper.getMainLooper());

    ZebraWrapper(Context _context) {
      super(_context);
    }

    public void setEventSink(EventChannel.EventSink _sink) {
        sink = _sink;
    }

    @Override
    public void broadcast(final String eventName, final HashMap map) {
        map.put("eventName", eventName);
        handler.post(() -> {
            if (sink != null) {
                try {
                 sink.success(map);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
