package dev.fml.zebra123;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** Zebra123 */
public class Zebra123 implements FlutterPlugin, MethodCallHandler, StreamHandler {

  private static final ZebraDevice.Interfaces INTERFACE = ZebraDevice.Interfaces.unknown;

  private MethodChannel methodHandler;
  private EventChannel eventHandler;

  private ZebraDevice device;

  private Context context;

  private final String METHODCHANNEL = "dev.fml.zebra123/method";
  private final String EVENTCHANNEL = "dev.fml.zebra123/event";

  boolean supportsRfid = false;
  boolean supportsDatawedge = false;

  // returns the package name
  public static String getPackageName(Context context) {
    if (context == null) return "unknown";
    return context.getPackageName();
  }

  // returns the datawedge profile name
  public static String getProfileName(Context context) {
    if (context == null) return "unknown";
    return context.getPackageName() + "." + "profile";
  }

  // returns the datawedge intent action
  public static String getActionName(Context context) {
    if (context == null) return "unknown";
    return context.getPackageName() + "." + "ACTION";
  }

  // returns the log tag name
  public static String getTagName(Context context) {
    return getPackageName(context) + "." + "ZEBRA";
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {

    context = flutterPluginBinding.getApplicationContext();

    //if (methodHandler != null) methodHandler.setMethodCallHandler(null);
    methodHandler = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), METHODCHANNEL);
    methodHandler.setMethodCallHandler(this);

    eventHandler = new EventChannel(flutterPluginBinding.getBinaryMessenger(), EVENTCHANNEL);
    eventHandler.setStreamHandler(this);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    disconnect();
    methodHandler.setMethodCallHandler(null);
    eventHandler.setStreamHandler(null);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

    // get method
    ZebraDevice.Methods method = ZebraDevice.Methods.unknown;
    try {
      method = ZebraDevice.Methods.valueOf(call.method);
    }
    catch(Exception e) {}

    switch (method) {

      case track:
        if (device != null) {
          ZebraDevice.Requests request = ZebraDevice.Requests.unknown;
          ArrayList<String> list = new ArrayList<>();
          try {
            request = ZebraDevice.Requests.valueOf(argument(call,"request"));
            String tags = argument(call,"tags");
            if (tags!= null) list.addAll(Arrays.asList(tags.split(",")));
          }
          catch(Exception e) {}
          device.track(request, list);
        }
        break;

      case scan:
        if (device != null) {
          ZebraDevice.Requests request = ZebraDevice.Requests.unknown;
          try {
            request = ZebraDevice.Requests.valueOf(argument(call,"request"));
          }
          catch(Exception e) {}
          device.scan(request);
        }
        break;

      case mode:
        if (device != null) {
          ZebraDevice.Modes mode = ZebraDevice.Modes.mixed;
          try {
            mode = ZebraDevice.Modes.valueOf(argument(call,"mode"));
          }
          catch(Exception e) {}
          device.setMode(mode);
        }
        break;

      case write:
        if (device != null) {
          String epc         = argument(call,"epc");
          String newEpc      = argument(call,"epcNew");
          String password    = argument(call,"password");
          String newPassword = argument(call,"passwordNew");
          String data        = argument(call,"data");
          device.write(epc, newEpc, password, newPassword, data);
        }
        break;

      // not implemented
      default:
        Toast.makeText(context, "Method " + call.method + " not implemented", Toast.LENGTH_LONG).show();
    }

    result.success(null);
  }

  @Override
  public void onListen(Object arguments, EventChannel.EventSink sink) {

    // set connection support
    supportsRfid = ZebraRfid.isSupported(context);
    supportsDatawedge = ZebraDataWedge.isSupported(context);

    // notify device support
    HashMap<String, Object> map = new HashMap<>();
    map.put(ZebraDevice.Interfaces.rfidapi3.toString(),supportsRfid ? "true" : "false");
    map.put(ZebraDevice.Interfaces.datawedge.toString(),supportsDatawedge ? "true" : "false");
    sendEvent(sink, ZebraDevice.Events.support,map);

    // connect the device
    connect(sink);
  }

  @Override
  public void onCancel(Object arguments) {

    Log.w(getTagName(context), "cancelling listener");
  }

  String argument(MethodCall call, String key) {
    try {
      return call.argument(key).toString();
    }
    catch(Exception e) {
      return "";
    }
  }

  private void connect(EventSink sink) {

    try {

      // disconnect if already connected
      if (device != null) device.disconnect();
      device = null;

      // device supports rfid?
      if (supportsRfid) {
        device = new ZebraRfid(context, sink);
        device.connect();
      }

      // datawedge supported?
      else if (supportsDatawedge) {
        device = new ZebraDataWedge(context, sink);
        device.connect();
      }

      // no supported device
      else {
        HashMap<String, Object> map =new HashMap<>();
        map.put("status", ZebraDevice.ZebraConnectionStatus.error.toString());

        // notify device
        sendEvent(sink, ZebraDevice.Events.connectionStatus,map);
      }
    }
    catch(Exception e) {
        Log.e(getTagName(context), "Error connecting to device" + e.getMessage());
        sendEvent(sink, ZebraDevice.Events.error, ZebraDevice.toError("Error during connect()", e));
    }
  }

  private void disconnect() {
    if (device != null) {
      device.disconnect();
    }
  }

  public void sendEvent(final EventSink sink, final ZebraDevice.Events event, final HashMap map) {

    if (sink == null) {
      Log.e(getTagName(context), "Can't send notification to flutter. Sink is null");
      return;
    }

    try
    {
      map.put("eventSource", INTERFACE.toString());
      map.put("eventName", event.toString());
      sink.success(map);
    }
    catch (Exception e)
    {
      Log.e(getTagName(context), "Error sending notification to flutter. Error: " + e.getMessage());
    }
  }
}
