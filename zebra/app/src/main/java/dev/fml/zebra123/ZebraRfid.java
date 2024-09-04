package dev.fml.zebra123;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.zebra.rfid.api3.ACCESS_OPERATION_CODE;
import com.zebra.rfid.api3.ACCESS_OPERATION_STATUS;
import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.BATCH_MODE;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.SL_FLAG;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.TriggerInfo;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZebraRfid implements Readers.RFIDReaderEventHandler, ZebraDevice {

    private static final String TAG = Zebra123.PLUGIN;

    private static final ZebraInterfaces INTERFACE = ZebraInterfaces.zebraSdk;

    Context context;

    private static Readers readers;
    private static ReaderDevice readerDevice;
    private static RFIDReader reader;
    private IEventHandler eventHandler = new IEventHandler();

    private HashMap<String, TagInfo> tags = new HashMap<>();

    ZebraDeviceListener listener;

    ZebraRfid(Context context, ZebraDeviceListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public static boolean isSupported(Context context) {

        try {
            Readers readers = new Readers(context, ENUM_TRANSPORT.ALL);
            ArrayList<ReaderDevice> readersListArray = readers.GetAvailableRFIDReaderList();
            if (readersListArray.size() > 0) return true;
        }
        catch(Exception e) {
            Log.d(TAG, "Reader does not support RFID");
        }
        return false;
    }

    public void setPowerLevel(int level) {
        if (reader != null) {
            try {
                Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
                config.setTransmitPowerIndex(level);
                config.setrfModeTableIndex(0);
                config.setTari(0);
                reader.Config.Antennas.setAntennaRfConfig(1, config);
            }
            catch (Exception e) {
                Log.e(TAG,e.getMessage());
            }
        }
    }

    public void setMode(String mode) {
        if (reader != null) {
            try {
                ENUM_TRIGGER_MODE _mode = ENUM_TRIGGER_MODE.RFID_MODE;
                if (mode.toLowerCase().trim().equals("barcode")) _mode = ENUM_TRIGGER_MODE.BARCODE_MODE;
                if (mode.toLowerCase().trim().equals("rfid")) _mode = ENUM_TRIGGER_MODE.RFID_MODE;
                setMode(_mode);
            }
            catch (Exception e) {
                Log.e(TAG,e.getMessage());
            }
        }
    }

    private void setMode(ENUM_TRIGGER_MODE mode) {
        if (reader != null) {
            try {
                reader.Config.setTriggerMode(mode, true);
            }
            catch (Exception e) {
                Log.e(TAG,e.getMessage());
            }
        }
    }

    public void setTriggers(START_TRIGGER_TYPE start, STOP_TRIGGER_TYPE stop) {
        if (reader != null) {
            try {
                TriggerInfo triggerInfo = new TriggerInfo();
                triggerInfo.StartTrigger.setTriggerType(start);
                triggerInfo.StopTrigger.setTriggerType(stop);
                reader.Config.setStartTrigger(triggerInfo.StartTrigger);
                reader.Config.setStopTrigger(triggerInfo.StopTrigger);
                reader.Config.setBatchMode(BATCH_MODE.ENABLE);
            }
            catch (Exception e) {
                Log.e(TAG,e.getMessage());
            }
        }
    }

    public void setAntennaConfig() {
        if (reader != null) {
            try {
                Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
                s1_singulationControl.setSession(SESSION.SESSION_S0);
                s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
                s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
                reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
            }
            catch (Exception e) {
                Log.e(TAG,e.getMessage());
            }
        }
    }

    public void connect() {

        Readers.attach(this);
        if (readers == null) {
            readers = new Readers(context,ENUM_TRANSPORT.ALL);
        }

        new AsyncTasks() {

            @Override
            public void doInBackground() {
                Log.d(TAG, "CreateInstanceTask");
                try {

                    if (readerDevice == null) {
                        ArrayList<ReaderDevice> readersListArray = readers.GetAvailableRFIDReaderList();
                        if (readersListArray.size() > 0) {
                            readerDevice = readersListArray.get(0);
                            reader = readerDevice.getRFIDReader();
                        } else {
                            Log.d(TAG,"No connectable device detected");
                        }
                    }

                    if (reader != null && !reader.isConnected()) {
                        reader.connect();

                        short[] allAntennas = new short[reader.ReaderCapabilities.getNumAntennaSupported()];
                        for(short i = 1; i<= reader.ReaderCapabilities.getNumAntennaSupported(); i++) {
                            allAntennas[i-1] = i;
                        }
                        reader.Actions.Inventory.perform(null, null, allAntennas);

                        ConfigureReader();
                    }

                }
                catch (Exception e)
                {
                    Log.d(TAG, e.getMessage());
                }
            }

            @Override
            public void onPostExecute() {

                HashMap<String, Object> map = new HashMap<>();
                map.put("status", ZebraConnectionStatus.connected.toString());

                // notify device
                if (listener != null) {
                    listener.notify(INTERFACE, ZebraEvents.connectionStatus,map);
                }
            }

            @Override
            public void onPreExecute() {
                // before execution
            }
        }.execute();
    }

    public void disconnect() {
        try {
            Readers.deattach(this);
            readerDevice=null;
            reader.Events.removeEventsListener();
            reader = null;
            if (readers != null) readers.Dispose();
            readers = null;

            HashMap<String, Object> map =new HashMap<>();
            map.put("status", ZebraConnectionStatus.disconnected.toString());

            // notify device
            if (listener != null) {
                listener.notify(INTERFACE, ZebraEvents.connectionStatus,map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isReaderConnected() {

        if (reader != null && reader.isConnected())
            return true;
        else {
            Log.d(TAG, "reader is not connected");
            return false;
        }
    }

    private synchronized void ConfigureReader() {
        Log.d(TAG, "ConfigureReader " + reader.getHostName());
        if (reader.isConnected()) {

            try {

                reader.Config.resetFactoryDefaults();

                // receive events from reader
                reader.Events.addEventsListener(eventHandler);
                reader.Events.setHandheldEvent(true);
                reader.Events.setTagReadEvent(true);
                reader.Events.setAttachTagDataWithReadEvent(false);

                // set read mode
                setMode(ENUM_TRIGGER_MODE.BARCODE_MODE);

                // set start and stop triggers
                setTriggers(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE, STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);

                // power levels are index based so maximum power supported get the last one
                int powerLevel =  reader.ReaderCapabilities.getTransmitPowerLevelValues().length - 1;
                setPowerLevel(powerLevel);

                // Set the singulation control
                setAntennaConfig();

                // delete any prefilters
                reader.Actions.PreFilters.deleteAll();

            } catch (InvalidUsageException | OperationFailureException e) {
                e.printStackTrace();
            }
        }
    }

    ///Get reader information
    public   ArrayList<ReaderDevice> getReadersList() {

        ArrayList<ReaderDevice> readersListArray=new  ArrayList<ReaderDevice>();

        try
        {
            if(readers!=null) {
                readersListArray = readers.GetAvailableRFIDReaderList();
                return readersListArray;
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "Error in getReadersList" + e.getMessage());
            if (listener != null) listener.notify(INTERFACE, ZebraEvents.error, ZebraDevice.toError("getReadersList()", e));
        }

        return  readersListArray;
    }


    public class IEventHandler implements RfidEventsListener {

        @Override
        public void eventReadNotify(RfidReadEvents rfidReadEvents) {

            // Recommended to use new method getReadTagsEx for better performance in case of large tag population
            TagData[] myTags = reader.Actions.getReadTags(100);
            if (myTags != null) {
                for (int index = 0; index < myTags.length; index++) {
                    TagData tagData=myTags[index];
                    ///read operation
                    if(tagData.getOpCode()==null || tagData.getOpCode()== ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ){
                        //&&tagData.getOpStatus()== ACCESS_OPERATION_STATUS.ACCESS_SUCCESS
                        TagInfo data=new TagInfo();
                        data.id=tagData.getTagID();
                        data.antenna=tagData.getAntennaID();
                        data.rssi =tagData.getPeakRSSI();
                        data.status =tagData.getOpStatus();
                        data.size=tagData.getTagIDAllocatedSize();
                        data.lockData=tagData.getPermaLockData();
                        if(tagData.isContainsLocationInfo()){
                            data.distance = tagData.LocationInfo.getRelativeDistance();
                        }
                        data.memoryBankData=tagData.getMemoryBankData();
                        tags.put(data.id, data);
                    }
                }
            }
        }

        @Override
        public void eventStatusNotify(RfidStatusEvents event) {

            Log.d(TAG, "Status Notification: " + event.StatusEventData.getStatusEventType());

            if (event.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {

                if (event.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED)
                {

                    new AsyncTasks() {

                        @Override
                        public void onPreExecute() {
                            // before execution
                        }

                        @Override
                        public void doInBackground() {
                            performInventory();
                        }

                        @Override
                        public void onPostExecute() {
                            // Ui task here
                        }

                    }.execute();
                }

                if (event.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {

                    new AsyncTasks() {

                        @Override
                        public void onPreExecute() {
                            // before execution
                        }

                        @Override
                        public void doInBackground() {
                            stopInventory();
                        }

                        @Override
                        public void onPostExecute() {
                            // Ui task here
                        }

                    }.execute();
                }
            }
        }
    }

    synchronized void performInventory() {
        // check reader connection
        if (!isReaderConnected())
            return;
        try
        {
            reader.Actions.Inventory.perform();
        }
        catch (InvalidUsageException e)
        {
            e.printStackTrace();
        }
        catch (OperationFailureException e)
        {
            e.printStackTrace();
        }
    }

    synchronized void stopInventory() {
        // check reader connection
        if (!isReaderConnected())
            return;
        try
        {
            reader.Actions.Inventory.stop();

            if (tags.size() > 0) {

                ArrayList<HashMap<String, Object>> data = new ArrayList<>();
                for (TagInfo tag : tags.values())
                    data.add(transitionEntity(tag));
                tags.clear();

                HashMap<String,Object> hashMap=new HashMap<>();
                hashMap.put("tags",data);

                // notify listener
                if (listener != null) {
                    listener.notify(INTERFACE, ZebraEvents.readRfid,hashMap);
                }
            }
        }
        catch (InvalidUsageException e) {
            e.printStackTrace();
        }
        catch (OperationFailureException e)
        {
            e.printStackTrace();
        }
    }


    @Override
    public void RFIDReaderAppeared(ReaderDevice readerDevice) {
        Log.d(TAG, "RFIDReaderAppeared " + readerDevice.getName());
//        new ConnectionTask().execute();
    }

    @Override
    public void RFIDReaderDisappeared(ReaderDevice readerDevice) {
        Log.d(TAG, "RFIDReaderDisappeared " + readerDevice.getName());
//        if (readerDevice.getName().equals(reader.getHostName()))
//            disconnect();
        disconnect();
    }

    //Entity class transfer HashMap
    public static HashMap<String, Object> transitionEntity(Object onClass) {
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        Field[] fields = onClass.getClass().getDeclaredFields();
        for (Field field : fields) {
            //Make private variables accessible during reflection
            field.setAccessible(true);
            try {
                hashMap.put(field.getName(), field.get(onClass));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return hashMap;
    }

    private static class TagInfo {

        public String id;
        public short antenna;
        public short rssi;
        public ACCESS_OPERATION_STATUS status;
        public short distance;
        public String memoryBankData;
        public String lockData;
        public int size;
        public String seen;

        TagInfo() {
            Date datetime = Calendar.getInstance().getTime();
            String date   = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS").format(datetime).toString();
            this.seen = date;
        }
    }

    public abstract class AsyncTasks {

        private final ExecutorService executors;

        public AsyncTasks() {
            this.executors = Executors.newSingleThreadExecutor();
        }

        private void startBackground() {
            onPreExecute();
            executors.execute(() -> {
                doInBackground();
                new Handler(Looper.getMainLooper()).post(() -> onPostExecute());
            });
        }

        public void execute() {
            startBackground();
        }

        public void shutdown() {
            executors.shutdown();
        }

        public boolean isShutdown() {
            return executors.isShutdown();
        }

        public abstract void onPreExecute();

        public abstract void doInBackground();

        public abstract void onPostExecute();
    }
}
