package dev.fml.zebra123;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

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

  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel methodHandler;
  private EventChannel eventHandler;

  private Handler handler = new Handler(Looper.getMainLooper());

  private EventChannel.EventSink sink = null;

  private ZebraDevice device;

  private Context context;

  private final String METHODCHANNEL = "dev.fml.zebra123/method";
  private final String EVENTCHANNEL = "dev.fml.zebra123/event";

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {

    context = flutterPluginBinding.getApplicationContext();

    methodHandler = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), METHODCHANNEL);
    methodHandler.setMethodCallHandler(this);

    eventHandler = new EventChannel(flutterPluginBinding.getBinaryMessenger(), EVENTCHANNEL);
    eventHandler.setStreamHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

    String method = call.method;

    switch (method) {

      case "getPlatformVersion":
        result.success("Android " + android.os.Build.VERSION.RELEASE);
        break;

      case "toast":
        Toast.makeText(context, call.argument("text"), Toast.LENGTH_LONG).show();
        break;

      case "connect":
        ZebraDevice.ZebraConnectionMethod _method = ZebraDevice.ZebraConnectionMethod.either;
        try {
          String param = call.argument("method");
          if (param != null) ZebraDevice.ZebraConnectionMethod.valueOf(param);
        }
        catch (Exception e) {
          _method = ZebraDevice.ZebraConnectionMethod.either;
        }

        // disconnect if already connected
        if (device != null) device.disconnect();
        device = null;

        boolean tryWedge = _method != ZebraDevice.ZebraConnectionMethod.sdk;
        boolean trySdk   = _method != ZebraDevice.ZebraConnectionMethod.wedge;

        // device supports rfid?
        if (trySdk && ZebraRfid.isSupported(context)) {
          device = new ZebraRfid(context, this);
          device.connect();
        }

        // datawedge supported?
        else if (tryWedge && ZebraDataWedge.isSupported(context)) {
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
        break;

      // set device mode
      case "mode":

        String mode = call.argument("mode");
        if (device != null) device.setMode(mode);
        break;

      // disconnect from the device
      case "disconnect":
        if (device != null) device.disconnect();
        result.success(null);
        break;

      // not implemented
      default:
        Toast.makeText(context, "Method not implemented: " + method, Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    methodHandler.setMethodCallHandler(null);
    eventHandler.setStreamHandler(null);
    if (device != null) device.dispose();
  }


  @Override
  public void onListen(Object arguments, EventChannel.EventSink sink) {
    Log.w(TAG, "adding listener");
    this.sink = sink;
  }

  @Override
  public void onCancel(Object arguments) {
    Log.w(TAG, "cancelling listener");
    sink = null;
  }

  @Override
  public void notify(final ZebraDevice.ZebraInterfaces source, final ZebraDevice.ZebraEvents event, final HashMap map) {

    map.put("eventSource", source.toString());
    map.put("eventName", event.toString());

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
