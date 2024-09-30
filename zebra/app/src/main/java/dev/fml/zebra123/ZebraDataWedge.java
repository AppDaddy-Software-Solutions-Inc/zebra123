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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public final class ZebraDataWedge extends BroadcastReceiver implements ZebraDevice {

    private static final String TAG = "zebra123";

    private static final ZebraInterfaces INTERFACE = ZebraInterfaces.datawedge;


    private Context context;
    ZebraDeviceListener listener;

    public static final String PROFILE_INTENT_ACTION = TAG;
    public static final String PROFILE_INTENT_BROADCAST = "2";
    public static final String DATAWEDGE_SEND_ACTION = "com.symbol.datawedge.api.ACTION";
    public static final String DATAWEDGE_RETURN_ACTION = "com.symbol.datawedge.api.RESULT_ACTION";
    public static final String DATAWEDGE_RETURN_CATEGORY = "android.intent.category.DEFAULT";
    public static final String DATAWEDGE_EXTRA_SEND_RESULT = "SEND_RESULT";
    public static final String DATAWEDGE_SCAN_EXTRA_DATA_STRING = "com.symbol.datawedge.data_string";
    public static final String DATAWEDGE_SCAN_EXTRA_SOURCE = "com.symbol.datawedge.source";
    public static final String DATAWEDGE_SCAN_EXTRA_LABEL_TYPE = "com.symbol.datawedge.label_type";
    public static final String DATAWEDGE_SEND_CREATE_PROFILE = "com.symbol.datawedge.api.CREATE_PROFILE";
    public static final String DATAWEDGE_SEND_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG";
    public static final String DATAWEDGE_SEND_SCANNER_COMMAND = "com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN";

    public ZebraDataWedge(Context context, ZebraDeviceListener listener) {

        this.context = context;
        this.listener = listener;

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
            Log.d(TAG, "Reader does not support Data Wedge");
        }
        return false;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(TAG)) {

            try {
                String data   = intent.getStringExtra("com.symbol.datawedge.data_string");
                String format = intent.getStringExtra("com.symbol.datawedge.label_type");
                Date datetime = Calendar.getInstance().getTime();
                String date   = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS").format(datetime).toString();

                // create a map of simple objects
                HashMap<String, Object> tag = new HashMap<>();
                tag.put("barcode", data);
                tag.put("format", format);
                tag.put("seen", date);

                // notify listener
                Log.d(TAG, ZebraEvents.readBarcode + ": " + tag);
                if (listener != null) {
                    listener.notify(INTERFACE, ZebraEvents.readBarcode, tag);
                }
            }
            catch(Exception e) {
                Log.e(TAG, "Error deserializing json object" + e.getMessage());
                if (listener != null) listener.notify(INTERFACE, ZebraEvents.error, ZebraDevice.toError("onReceive()", e));
            }{}
        }
    }


    private void sendCommandString(@NotNull String command, @NotNull String parameter, boolean sendResult) {
        Intent dwIntent = new Intent();
        dwIntent.setAction("com.symbol.datawedge.api.ACTION");
        dwIntent.putExtra(command, parameter);
        if (sendResult) {
            dwIntent.putExtra("SEND_RESULT", "true");
        }
        this.context.sendBroadcast(dwIntent);
    }

    private void sendCommandBundle(@NotNull String command, @NotNull Bundle parameter) {
        Intent dwIntent = new Intent();
        dwIntent.setAction("com.symbol.datawedge.api.ACTION");
        dwIntent.putExtra(command, parameter);
        this.context.sendBroadcast(dwIntent);
    }

    @Override
    public void connect() {

        try {

            final IntentFilter filter = new IntentFilter();
            filter.addCategory(Intent.CATEGORY_DEFAULT);
            filter.addAction(TAG); // Please use this String in your DataWedge profile configuration
            context.registerReceiver(this, filter);

            HashMap<String, Object> map =new HashMap<>();
            map.put("status", ZebraConnectionStatus.connected.toString());

            // notify device
            if (listener != null) {
                listener.notify(INTERFACE, ZebraEvents.connectionStatus,map);
            }
        }
        catch(Exception e) {
            Log.e(TAG, "Error connecting to device" + e.getMessage());
            if (listener != null) listener.notify(INTERFACE, ZebraEvents.error, ZebraDevice.toError("connect()", e));
        }
    }

    @Override
    public void disconnect() {

        try {
            context.unregisterReceiver(this);

            HashMap<String, Object> map =new HashMap<>();
            map.put("status", ZebraConnectionStatus.disconnected.toString());

            // notify device
            if (listener != null) {
                listener.notify(INTERFACE, ZebraEvents.connectionStatus,map);
            }
        }
        catch(Exception e) {
            Log.e(TAG, "Error disconnecting from device" + e.getMessage());
            if (listener != null) listener.notify(INTERFACE, ZebraEvents.error, ZebraDevice.toError("disconnect()", e));
        }
    }

    @Override
    public void dispose() {
        listener = null;
        context.unregisterReceiver(this);
    }

    @Override
    public void scan(ZebraScanRequest request) {
        Exception exception = new Exception("Not implemented");
        if (listener != null) listener.notify(INTERFACE, ZebraEvents.writeFail, ZebraDevice.toError("Error writing tag data", exception));
        return;
    }

    @Override
    public void track(ZebraScanRequest request, ArrayList<String> tags) {
        Exception exception = new Exception("Not implemented");
        if (listener != null) listener.notify(INTERFACE, ZebraEvents.writeFail, ZebraDevice.toError("Error writing tag data", exception));
        return;
    }

    @Override
    public void setMode(String mode) {

    }

    @Override
    public void write(String epc, String newEpc, String password, String newPassword, String data) {
        Exception exception = new Exception("Not implemented");
        if (listener != null) listener.notify(INTERFACE, ZebraEvents.writeFail, ZebraDevice.toError("Error writing tag data", exception));
        return;
    }

    private void createProfile() {

        String profile = context.getPackageName() + "." + TAG;

        // create the profile if it doesnt exist
        sendCommandString("com.symbol.datawedge.api.CREATE_PROFILE", profile, false);

        Bundle dwProfile = new Bundle();
        dwProfile.putString("PROFILE_NAME", profile);
        dwProfile.putString("PROFILE_ENABLED", "true");
        dwProfile.putString("CONFIG_MODE", "UPDATE");

        Bundle appConfig = new Bundle();
        appConfig.putString("PACKAGE_NAME", context.getPackageName());

        String[] var4 = new String[]{"*"};
        appConfig.putStringArray("ACTIVITY_LIST", var4);
        Bundle[] var13 = new Bundle[]{appConfig};

        dwProfile.putParcelableArray("APP_LIST", (Parcelable[])var13);

        ArrayList plugins = new ArrayList();

        Bundle intentPluginProperyties = new Bundle();
        intentPluginProperyties.putString("intent_output_enabled", "true");
        intentPluginProperyties.putString("intent_action", TAG);
        intentPluginProperyties.putString("intent_delivery", "2");

        Bundle intentPlugin = new Bundle();
        intentPlugin.putString("PLUGIN_NAME", "INTENT");
        intentPlugin.putString("RESET_CONFIG", "true");
        intentPlugin.putBundle("PARAM_LIST", intentPluginProperyties);
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
        this.sendCommandBundle("com.symbol.datawedge.api.SET_CONFIG", dwProfile);

        sendCommandString("com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN", "ENABLE_PLUGIN", false);
    }
}
