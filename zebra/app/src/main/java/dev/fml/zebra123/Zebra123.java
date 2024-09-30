package dev.fml.zebra123;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** Zebra123 */
public class Zebra123 implements FlutterPlugin, MethodCallHandler, StreamHandler, ZebraDeviceListener {

  public  final static String PLUGIN = "zebra123";
  private final static String TAG = PLUGIN;

  private static final ZebraDevice.ZebraInterfaces INTERFACE = ZebraDevice.ZebraInterfaces.unknown;

  private static MethodChannel methodHandler;
  private static EventChannel eventHandler;

  private Handler handler = new Handler(Looper.getMainLooper());

  private EventChannel.EventSink sink = null;

  private ZebraDevice device;

  private Context context;

  private final String METHODCHANNEL = "dev.fml.zebra123/method";
  private final String EVENTCHANNEL = "dev.fml.zebra123/event";

  boolean supportsRfid = false;
  boolean supportsDatawedge = false;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {

    context = flutterPluginBinding.getApplicationContext();

    if (methodHandler != null) methodHandler.setMethodCallHandler(null);
    methodHandler = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), METHODCHANNEL);
    methodHandler.setMethodCallHandler(this);

    if (eventHandler != null) eventHandler.setStreamHandler(null);
    eventHandler = new EventChannel(flutterPluginBinding.getBinaryMessenger(), EVENTCHANNEL);
    eventHandler.setStreamHandler(this);

    // set connection support
    supportsRfid = ZebraRfid.isSupported(context);
    supportsDatawedge = ZebraDataWedge.isSupported(context);

    connect();
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {

    methodHandler.setMethodCallHandler(null);
    methodHandler = null;

    eventHandler.setStreamHandler(null);
    eventHandler = null;

    //disconnect();
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

    String method = call.method;

    switch (method) {

      case "connect":
        connect();
        break;

      // disconnect from the device
      case "disconnect":
        break;

      case "track":
        if (device != null) {
          ZebraDevice.ZebraScanRequest request = ZebraDevice.ZebraScanRequest.unknown;
          ArrayList<String> list = new ArrayList<>();
          try {
            request = ZebraDevice.ZebraScanRequest.valueOf(argument(call,"request"));
            String tags = argument(call,"tags");
            if (tags!= null) list.addAll(Arrays.asList(tags.split(",")));
          }
          catch(Exception e) {}
          device.track(request, list);
        }
        break;

      case "scan":
        if (device != null) {
          ZebraDevice.ZebraScanRequest request = ZebraDevice.ZebraScanRequest.unknown;
          try {
            request = ZebraDevice.ZebraScanRequest.valueOf(argument(call,"request"));
          }
          catch(Exception e) {}
          device.scan(request);
        }
        break;

      case "write":
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
        Toast.makeText(context, "Method not implemented: " + method, Toast.LENGTH_LONG).show();
    }

    result.success(null);
  }

  String argument(MethodCall call, String key) {
    try {
      return call.argument(key).toString();
    }
    catch(Exception e) {
      return "";
    }
  }
  private void connect() {

    try {

      // disconnect if already connected
      //if (device != null) device.disconnect();
      device = null;

      // device supports rfid?
      if (supportsRfid) {
        device = new ZebraRfid(context, this);
        device.connect();
      }

      // datawedge supported?
      else if (supportsDatawedge) {
        device = new ZebraDataWedge(context, this);
        device.connect();
      }

      // no supported device
      else {
        HashMap<String, Object> map =new HashMap<>();
        map.put("status", ZebraDevice.ZebraConnectionStatus.error.toString());

        // notify device
        notify(INTERFACE, ZebraDevice.ZebraEvents.connectionStatus,map);
      }
    }
    catch(Exception e) {
        Log.e(TAG, "Error connecting to device" + e.getMessage());
        notify(INTERFACE, ZebraDevice.ZebraEvents.error, ZebraDevice.toError("Error during connect()", e));
    }
  }

  private void disconnect() {
    if (device != null) {
      device.disconnect();
    }
  }

  @Override
  public void onListen(Object arguments, EventChannel.EventSink sink) {

    Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>> Setting Sink");
    this.sink = sink;


    // notify device support
    HashMap<String, Object> map =new HashMap<>();
    map.put(ZebraDevice.ZebraInterfaces.rfidapi3.toString(),supportsRfid ? "true" : "false");
    map.put(ZebraDevice.ZebraInterfaces.datawedge.toString(),supportsDatawedge ? "true" : "false");
    notify(INTERFACE, ZebraDevice.ZebraEvents.support,map);
  }

  @Override
  public void onCancel(Object arguments) {
    Log.w(TAG, "cancelling listener");
    sink = null;
  }

  @Override
  public void notify(final ZebraDevice.ZebraInterfaces source, final ZebraDevice.ZebraEvents event, final HashMap map) {

    if (sink == null) Log.e(TAG, "Can't send notification to flutter. Sink is null");

    if (sink != null) {
      handler.post(() -> {
          try
          {
            map.put("eventSource", source.toString());
            map.put("eventName", event.toString());
            sink.success(map);
          }
          catch (Exception e)
          {
            Log.e(TAG, "Error sending notification to flutter");
          }
      });
    }
  }

}
