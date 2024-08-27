package dev.fml.zebra123;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** Zebra123 */
public class Zebra123 implements FlutterPlugin, MethodCallHandler, StreamHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel method;
  private EventChannel event;
  private EventChannel.EventSink sink = null;

  private ZebraWrapper zebra;
  private Context context;

  private final String TAG = "Zebra123";
  public  final String METHODCHANNEL = "dev.fml.zebra123/method";
  public  final String EVENTCHANNEL = "dev.fml.zebra123/event";

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    context = flutterPluginBinding.getApplicationContext();
    zebra = new ZebraWrapper(context);

    method = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), METHODCHANNEL);
    method.setMethodCallHandler(this);

    event = new EventChannel(flutterPluginBinding.getBinaryMessenger(), EVENTCHANNEL);
    event.setStreamHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

    switch (call.method) {

      case "getPlatformVersion":
        result.success("Android " + android.os.Build.VERSION.RELEASE);
        break;

      case "toast":
        String txt=call.argument("text");
        Toast.makeText(context, txt, Toast.LENGTH_LONG).show();
        break;

      case "connect":
        // boolean  isBluetooth=call.argument("isBluetooth");
        zebra.connect();
        break;

      case "mode":
        String mode = call.argument("mode");
        zebra.setReadMode(mode);
        break;

      case "disconnect":
        zebra.dispose();
        result.success(null);
        break;

      case "getReadersList":
        zebra.getReadersList();
//        break;
      case "write":
        break;
      default:
        result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    method.setMethodCallHandler(null);
    event.setStreamHandler(null);
  }


  @Override
  public void onListen(Object arguments, EventChannel.EventSink sink) {
    Log.w(TAG, "adding listener");
    this.sink = sink;
    zebra.setEventSink(sink);
  }

  @Override
  public void onCancel(Object arguments) {
    Log.w(TAG, "cancelling listener");
    sink = null;
  }
}
