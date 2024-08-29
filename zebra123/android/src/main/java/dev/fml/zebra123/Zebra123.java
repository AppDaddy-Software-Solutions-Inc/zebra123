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
public class Zebra123 implements FlutterPlugin, MethodCallHandler, StreamHandler, ZebraListener {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel oMethodHandler;
  private EventChannel  oEventHandler;

  public Handler oHandler = new Handler(Looper.getMainLooper());

  private EventChannel.EventSink sink = null;

  private ZebraDataWedge oZebraDataWedge;
  private ZebraSdk oZebraSdk;

  private Context oContext;

  private final String TAG = "Zebra123";
  public  final String METHODCHANNEL = "dev.fml.zebra123/method";
  public  final String EVENTCHANNEL = "dev.fml.zebra123/event";

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {

    oContext = flutterPluginBinding.getApplicationContext();

    oZebraSdk = new ZebraSdk(oContext, this);
    //oZebraDataWedge = new ZebraDataWedge(oContext, this);

    oMethodHandler = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), METHODCHANNEL);
    oMethodHandler.setMethodCallHandler(this);

    oEventHandler = new EventChannel(flutterPluginBinding.getBinaryMessenger(), EVENTCHANNEL);
    oEventHandler.setStreamHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

    switch (call.method) {

      case "getPlatformVersion":
        result.success("Android " + android.os.Build.VERSION.RELEASE);
        break;

      case "toast":
        String txt=call.argument("text");
        Toast.makeText(oContext, txt, Toast.LENGTH_LONG).show();
        break;

      case "connect":
        // boolean  isBluetooth=call.argument("isBluetooth");
        oZebraSdk.connect();
        break;

      case "mode":
        String mode = call.argument("mode");
        oZebraSdk.setReadMode(mode);
        break;

      case "disconnect":
        oZebraSdk.dispose();
        result.success(null);
        break;

      case "getReadersList":
        oZebraSdk.getReadersList();
//        break;
      case "write":
        break;
      default:
        result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    oMethodHandler.setMethodCallHandler(null);
    oEventHandler.setStreamHandler(null);
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
  public void onZebraListenerSuccess(final String event, final HashMap map) {

    map.put("eventName", event);
    oHandler.post(() -> {
      if (sink != null) {
        try {
          sink.success(map);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  @Override
  public void onZebraListenerError(final String event, final HashMap map) {

  }
}
