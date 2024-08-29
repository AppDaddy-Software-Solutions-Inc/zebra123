package dev.fml.zebra123;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;

import io.flutter.plugin.common.EventChannel;

public final class ZebraDataWedge extends BroadcastReceiver {

    private Context context;
    ZebraListener listener;

    public static final String PROFILE = "dev.fml.zebra123";

    public static final String PROFILE_INTENT_ACTION = PROFILE;
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

    public ZebraDataWedge(Context context, ZebraListener listener) {

        this.context = context;
        this.listener = listener;

        this.createProfile();

        final IntentFilter filter = new IntentFilter();
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addAction(PROFILE); // Please use this String in your DataWedge profile configuration
        context.registerReceiver(this, filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(PROFILE)) {

            //Get data from Intent
            String decodedSource = intent.getStringExtra("com.symbol.datawedge.source");
            String decodedData = intent.getStringExtra("com.symbol.datawedge.data_string");
            String decodedLabelType = intent.getStringExtra("com.symbol.datawedge.label_type");

            // create a json object which will be returned to Flutter part
            JSONObject json = new JSONObject();
            try{
                json.put("decodedSource",decodedSource);
                json.put("decodedData",decodedData);
                json.put("decodedLabelType",decodedLabelType);
                //if (listener != null) listener.onZebraListenerSuccess(json.);
            }catch(Exception e){
                // catch json exceptions
                //sink.success(e.toString());
            }
        }
    }


    public void sendCommandString(@NotNull String command, @NotNull String parameter, boolean sendResult) {
        Intent dwIntent = new Intent();
        dwIntent.setAction("com.symbol.datawedge.api.ACTION");
        dwIntent.putExtra(command, parameter);
        if (sendResult) {
            dwIntent.putExtra("SEND_RESULT", "true");
        }
        this.context.sendBroadcast(dwIntent);
    }

    public void sendCommandBundle(@NotNull String command, @NotNull Bundle parameter) {
        Intent dwIntent = new Intent();
        dwIntent.setAction("com.symbol.datawedge.api.ACTION");
        dwIntent.putExtra(command, parameter);
        this.context.sendBroadcast(dwIntent);
    }

    public void createProfile() {

        // create the profile if it doesnt exist
        sendCommandString("com.symbol.datawedge.api.CREATE_PROFILE", PROFILE, false);

        Bundle dwProfile = new Bundle();
        dwProfile.putString("PROFILE_NAME", PROFILE);
        dwProfile.putString("PROFILE_ENABLED", "true");
        dwProfile.putString("CONFIG_MODE", "UPDATE");

        Bundle appConfig = new Bundle();
        appConfig.putString("PACKAGE_NAME", PROFILE);

        String[] var4 = new String[]{"*"};
        appConfig.putStringArray("ACTIVITY_LIST", var4);
        Bundle[] var13 = new Bundle[]{appConfig};

        dwProfile.putParcelableArray("APP_LIST", (Parcelable[])var13);

        ArrayList plugins = new ArrayList();

        Bundle intentPluginProperyties = new Bundle();
        intentPluginProperyties.putString("intent_output_enabled", "true");
        intentPluginProperyties.putString("intent_action", PROFILE);
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
