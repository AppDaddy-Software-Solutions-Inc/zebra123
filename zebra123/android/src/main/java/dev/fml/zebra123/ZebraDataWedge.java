package dev.fml.zebra123;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.EventChannel.EventSink;

public class ZebraDataWedge extends BroadcastReceiver implements ZebraDevice {

    private static final Interfaces INTERFACE = Interfaces.datawedge;

    private Context context;
    private EventSink sink = null;

    public static String barcodeLast = "";
    public static long seenLast = 0;

     public ZebraDataWedge(Context context, EventSink sink) {
        this.context = context;
        this.sink = sink;
        this.createProfile();
    }

    public static boolean isSupported(Context context) {
        try {
            String deviceName = Build.MANUFACTURER;

            Intent i = new Intent();
            i.setAction("com.symbol.datawedge.api.ACTION");
            i.putExtra("com.symbol.datawedge.api.GET_VERSION_INFO", "");
            context.sendBroadcast(i);
            return true;
        }
        catch(Exception e) {
            Log.d(Zebra123.getTagName(context), "Reader does not support Data Wedge");
        }
        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String actionSource = intent.getAction();
        String actionTarget = Zebra123.getActionName(context);
        if (actionSource.equals(actionTarget)) {
            try {
                String barcode = intent.getStringExtra("com.symbol.datawedge.data_string");
                String format  = intent.getStringExtra("com.symbol.datawedge.label_type");
                long   seen    = System.currentTimeMillis();
                String date    = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS").format(new Date(seen)).toString();

                // create a map of simple objects
                HashMap<String, Object> tag = new HashMap<>();
                tag.put("barcode", barcode);
                tag.put("format", format);
                tag.put("seen", date);

                // duplicate reads within 1 second are ignored
                if (barcode.equals(barcodeLast) && Math.abs(seen - seenLast) < 1000) {
                    Log.e(Zebra123.getTagName(context), "Duplicate barcode read within 1 second. Skipping.");
                    return;
                }
                barcodeLast = barcode;
                seenLast = seen;

                // notify listener
                Log.d(Zebra123.getTagName(context), Events.readBarcode + ": " + tag);

                sendEvent(Events.readBarcode, tag);
            }
            catch(Exception e) {
                Log.e(Zebra123.getTagName(context), "Error deserializing json object" + e.getMessage());
                sendEvent(Events.error, ZebraDevice.toError("onReceive()", e));
            }
        }
    }


    public static void send(Context context, String extraKey, Bundle extraValue) {
        try
        {
            Intent dwIntent = new Intent();
            dwIntent.setAction("com.symbol.datawedge.api.ACTION");
            dwIntent.putExtra(extraKey, extraValue);
            context.sendBroadcast(dwIntent);
        }
        catch (Exception e)
        {
            Log.e(Zebra123.getTagName(context), "Error sending command to device" + e.getMessage());
        }
    }


    public static void send(Context context, String extraKey, String extraValue) {
        try
        {
            Intent dwIntent = new Intent();
            dwIntent.setAction("com.symbol.datawedge.api.ACTION");
            dwIntent.putExtra(extraKey, extraValue);
            context.sendBroadcast(dwIntent);
        }
        catch (Exception e)
        {
            Log.e(Zebra123.getTagName(context), "Error sending command to device" + e.getMessage());
        }
    }

    @Override
    public void connect() {

        try {
            final IntentFilter filter = new IntentFilter();
            //filter.addAction("com.symbol.datawedge.api.RESULT_ACTION");
            //filter.addAction("com.symbol.datawedge.api.ACTION");
            //filter.addAction("com.symbol.datawedge.api.NOTIFICATION_ACTION");
            filter.addAction(Zebra123.getActionName(context));
            filter.addCategory(Intent.CATEGORY_DEFAULT);

            context.registerReceiver(this, filter);

            HashMap<String, Object> map =new HashMap<>();
            map.put("status", ZebraConnectionStatus.connected.toString());

            // notify device
            sendEvent(Events.connectionStatus,map);
        }
        catch(Exception e) {

            Log.e(Zebra123.getTagName(context), "Error connecting to device" + e.getMessage());
            sendEvent(Events.error, ZebraDevice.toError("connect()", e));
        }
    }

    @Override
    public void disconnect() {

        try {
            context.unregisterReceiver(this);

            HashMap<String, Object> map =new HashMap<>();
            map.put("status", ZebraConnectionStatus.disconnected.toString());

            // notify device
            sendEvent(Events.connectionStatus,map);
        }
        catch(Exception e) {
            Log.e(Zebra123.getTagName(context), "Error disconnecting from device" + e.getMessage());
            sendEvent(Events.error, ZebraDevice.toError("disconnect()", e));
        }
    }

    @Override
    public void dispose() {
        try {
            context.unregisterReceiver(this);
        }
        catch(Exception e)
        {
            Log.e(Zebra123.getTagName(context), "Error during dispose()." + e.getMessage());
        }
    }

    @Override
    public void scan(Requests request) {
        // set the scanner to start or stop scanning
        send(context, "com.symbol.datawedge.api.SOFT_SCAN_TRIGGER", request == Requests.start ? "START_SCANNING" : "STOP_SCANNING");
        return;
    }

    @Override
    public void setMode(Modes mode) {
        Exception exception = new Exception("Not implemented");
        sendEvent(Events.error, ZebraDevice.toError("Error calling mode()", exception));
        return;
    }

    @Override
    public void track(Requests request, ArrayList<String> tags) {
        Exception exception = new Exception("Not implemented");
        sendEvent(Events.error, ZebraDevice.toError("Error calling track()", exception));
        return;
    }

    @Override
    public void write(String epc, String newEpc, String password, String newPassword, String data) {
        Exception exception = new Exception("Not implemented");
        sendEvent(Events.error, ZebraDevice.toError("Error calling write()", exception));
        return;
    }

    public void setMode(String mode) {
        Exception exception = new Exception("Not implemented");
        sendEvent(Events.error, ZebraDevice.toError("Error calling setMode()", exception));
        return;
    }

    private void createProfile() {

        try {

            String packageName = Zebra123.getPackageName(context);
            String profileName = Zebra123.getProfileName(context);
            String actionName  = Zebra123.getActionName(context);

            Log.i(Zebra123.getTagName(context), "Creating Datawedge profile " + profileName + " for package " + packageName + " with Intent action " + actionName);

            // create the profile if it doesnt exist
            send(context, "com.symbol.datawedge.api.CREATE_PROFILE", packageName);

            Bundle dwProfile = new Bundle();
            dwProfile.putString("PROFILE_NAME", profileName);
            dwProfile.putString("PROFILE_ENABLED", "true");
            dwProfile.putString("CONFIG_MODE", "UPDATE");

            Bundle appConfig = new Bundle();
            appConfig.putString("PACKAGE_NAME", packageName);

            appConfig.putStringArray("ACTIVITY_LIST", new String[]{"*"});
            dwProfile.putParcelableArray("APP_LIST", (Parcelable[]) new Bundle[]{appConfig});

            ArrayList plugins = new ArrayList();

            Bundle intentPluginProperties = new Bundle();
            intentPluginProperties.putString("intent_output_enabled", "true");
            intentPluginProperties.putString("intent_action", actionName);
            intentPluginProperties.putString("intent_delivery", "2");

            Bundle intentPlugin = new Bundle();
            intentPlugin.putString("PLUGIN_NAME", "INTENT");
            intentPlugin.putString("RESET_CONFIG", "true");
            intentPlugin.putBundle("PARAM_LIST", intentPluginProperties);
            plugins.add(intentPlugin);

            Bundle barcodePluginProperties = new Bundle();
            barcodePluginProperties.putString("scanner_input_enabled", "true");
            barcodePluginProperties.putString("scanner_selection", "auto");
            Bundle barcodePlugin = new Bundle();
            barcodePlugin.putString("PLUGIN_NAME", "BARCODE");
            barcodePlugin.putString("RESET_CONFIG", "true");
            barcodePlugin.putBundle("PARAM_LIST", barcodePluginProperties);
            plugins.add(barcodePlugin);

            Bundle rfidPluginProperties = new Bundle();
            rfidPluginProperties.putString("rfid_input_enabled", "true");
            rfidPluginProperties.putString("rfid_beeper_enable", "true");
            rfidPluginProperties.putString("rfid_led_enable", "true");
            rfidPluginProperties.putString("rfid_antenna_transmit_power", "30");
            rfidPluginProperties.putString("rfid_memory_bank", "0");
            rfidPluginProperties.putString("rfid_session", "1");
            rfidPluginProperties.putString("rfid_trigger_mode", "0");
            rfidPluginProperties.putString("rfid_filter_duplicate_tags", "true");
            rfidPluginProperties.putString("rfid_hardware_trigger_enabled", "true");
            rfidPluginProperties.putString("rfid_tag_read_duration", "1000");
            rfidPluginProperties.putString("rfid_link_profile", "0");
            rfidPluginProperties.putString("rfid_pre_filter_enable", "false");
            rfidPluginProperties.putString("rfid_post_filter_enable", "false");

            Bundle rfidPlugin = new Bundle();
            rfidPlugin.putString("PLUGIN_NAME", "RFID");
            rfidPlugin.putString("RESET_CONFIG", "true");
            rfidPlugin.putBundle("PARAM_LIST", rfidPluginProperties);
            plugins.add(rfidPlugin);

            Bundle keystrokePluginProperties = new Bundle();
            keystrokePluginProperties.putString("keystroke_output_enabled", "false");

            Bundle keystrokePlugin = new Bundle();
            keystrokePlugin.putString("PLUGIN_NAME", "KEYSTROKE");
            keystrokePlugin.putString("RESET_CONFIG", "true");
            keystrokePlugin.putBundle("PARAM_LIST", keystrokePluginProperties);
            plugins.add(keystrokePlugin);
            dwProfile.putParcelableArrayList("PLUGIN_CONFIG", plugins);
            this.send(context, "com.symbol.datawedge.api.SET_CONFIG", dwProfile);

            //sendCommandString("com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN", "ENABLE_PLUGIN", false);
        }
        catch (Exception e) {
            Log.e(Zebra123.getTagName(context), "Error creating profile" + e.getMessage());
        }
    }

    private void sendEvent(final ZebraDevice.Events event, final HashMap map) {

        if (sink == null) {
            Log.e(Zebra123.getTagName(context), "Can't send notification to flutter. Sink is null");
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
            Log.e(Zebra123.getTagName(context), "Error sending notification to flutter. Error: " + e.getMessage());
        }
    }
}
