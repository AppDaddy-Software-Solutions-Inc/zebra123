package dev.fml.zebra123;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.zebra.rfid.api3.ACCESS_OPERATION_CODE;
import com.zebra.rfid.api3.ACCESS_OPERATION_STATUS;
import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.BATCH_MODE;
import com.zebra.rfid.api3.BEEPER_VOLUME;
import com.zebra.rfid.api3.DYNAMIC_POWER_OPTIMIZATION;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.MEMORY_BANK;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RegionInfo;
import com.zebra.rfid.api3.RegulatoryConfig;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.SL_FLAG;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.TagAccess;
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

public class ZebraRfid implements ZebraDevice, RfidEventsListener {

    private static final String TAG = "zebra123";

    private static final ZebraInterfaces INTERFACE = ZebraInterfaces.rfidapi3;

    Context context;

    private RFIDReader reader;

    // holds a list of tags read
    private HashMap<String, TagInfo> tags = new HashMap<>();

    // holds a list of epc's to track
    private ArrayList<String> tracking = new ArrayList<>();

    ZebraDeviceListener listener;

    ZebraRfid(Context context, ZebraDeviceListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public static boolean isSupported(Context context) {

        try {
            Readers readers = new Readers(context, ENUM_TRANSPORT.ALL);
            if (readers.GetAvailableRFIDReaderList().size() > 0) return true;
        }
        catch(Exception e) {
            Log.d(TAG, "Reader does not support RFID");
        }
        return false;
    }

    private synchronized void ConfigureReader() {
        if (isReaderConnected()) {

            try {

                Log.d(TAG, "ConfigureReader()");

                // receive events from reader
                setEvents();

                // set read mode
                setMode(ENUM_TRIGGER_MODE.RFID_MODE);

                // set start and stop triggers
                setTriggers(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE, STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);

                // power levels are index based so maximum power supported get the last one
                int powerLevel =  reader.ReaderCapabilities.getTransmitPowerLevelValues().length - 1;
                setPowerLevel(powerLevel);

                reader.Config.setBeeperVolume(BEEPER_VOLUME.HIGH_BEEP);

                // Set the singulation control
                setAntennaConfig();

                // delete any prefilters
                reader.Actions.PreFilters.deleteAll();

            } catch (InvalidUsageException | OperationFailureException e) {
                e.printStackTrace();
            }
        }
    }

    private void setRegulatoryConfig() {

        try {
        if (reader != null) {
            Log.e(TAG,"Setting region");

            // Get and Set regulatory configuration settings
            RegulatoryConfig regulatoryConfig = reader.Config.getRegulatoryConfig();
            RegionInfo regionInfo = reader.ReaderCapabilities.SupportedRegions.getRegionInfo(1);
            regulatoryConfig.setRegion(regionInfo.getRegionCode());
            regulatoryConfig.setIsHoppingOn(regionInfo.isHoppingConfigurable());
            regulatoryConfig.setEnabledChannels(regionInfo.getSupportedChannels());
            reader.Config.setRegulatoryConfig(regulatoryConfig);
        }
        }
        catch(Exception e) {
            Log.e(TAG,"Error setting region");
        }
    }

    public void setPowerLevel(int level) {
        try
        {
            if (reader != null) {
                Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
                config.setTransmitPowerIndex(level);
                config.setrfModeTableIndex(0);
                config.setTari(0);
                reader.Config.Antennas.setAntennaRfConfig(1, config);
            }
        }
        catch (Exception e) {
            Log.e(TAG,e.getMessage());
        }
    }

    public void setEvents() {
        try {
            if (reader != null) {
                reader.Events.addEventsListener(this);
                reader.Events.setHandheldEvent(true);
                reader.Events.setTagReadEvent(true);
                reader.Events.setAttachTagDataWithReadEvent(true);

                // this will make the led's flash when a tag is read and while the trigger is held down
                reader.Config.setUniqueTagReport(false);
            }
        }
        catch (Exception e) {
            Log.e(TAG,e.getMessage());
        }
    }

    public void setMode(String mode) {
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

    private void setMode(ENUM_TRIGGER_MODE mode) {
        try {
            if (reader != null) {
                reader.Config.setTriggerMode(mode, true);
            }
        }
        catch (Exception e) {
            Log.e(TAG,e.getMessage());
        }
    }

    public void setTriggers(START_TRIGGER_TYPE start, STOP_TRIGGER_TYPE stop) {
        try {
            if (reader != null) {
                TriggerInfo triggerInfo = new TriggerInfo();
                triggerInfo.StartTrigger.setTriggerType(start);
                triggerInfo.StopTrigger.setTriggerType(stop);
                reader.Config.setStartTrigger(triggerInfo.StartTrigger);
                reader.Config.setStopTrigger(triggerInfo.StopTrigger);
                reader.Config.setBatchMode(BATCH_MODE.ENABLE);
            }
        }
        catch (Exception e) {
            Log.e(TAG,e.getMessage());
        }
    }

    public void setAntennaConfig() {
        try {
            if (reader != null) {
                Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
                s1_singulationControl.setSession(SESSION.SESSION_S0);
                s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
                s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
                reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
            }
        }
        catch (Exception e) {
            Log.e(TAG,e.getMessage());
        }
    }

    AsyncTasks connectionTask;

    public void connect() {

        Log.d(TAG,">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Connecting to RFID reader");

        if (connectionTask != null) {
            connectionTask.shutdown();
            connectionTask = null;
        }

        connectionTask = new AsyncTasks() {

            @Override
            public void doInBackground() {
                try {
                    if (reader == null) {
                        Readers readers = new Readers(context,ENUM_TRANSPORT.ALL);
                        ArrayList<ReaderDevice> rfidReaders = readers.GetAvailableRFIDReaderList();
                        if (rfidReaders.size() > 0) {
                            ReaderDevice device = rfidReaders.get(0);
                            reader = device.getRFIDReader();

                            //setRegulatoryConfig();
                        }
                        else {
                            Log.e(TAG,"No connectable rfid devices found");
                        }
                    }

                    if (reader != null) {
                        reader.connect();
                        ConfigureReader();
                    }
                }
                catch (Exception e)
                {
                    Log.d(TAG, e.toString());
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
        };

        connectionTask.execute();
    }

    public void disconnect() {
        try {

            if (reader != null) reader.Events.removeEventsListener(this);
            //reader = null;

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

    @Override
    public void scan(ZebraScanRequest request) {

        if (request == ZebraScanRequest.rfidStartScanning) {
            startScanning();
        }
        else if (request == ZebraScanRequest.rfidStopScanning) {
            stopScanning();
        }
    }

    public void track(ZebraScanRequest request, ArrayList<String> tags) {

        if (request == ZebraScanRequest.rfidStartTracking) {
            startTracking(tags);
        }
        else if (request == ZebraScanRequest.rfidStopTracking) {
            stopTracking();
        }
    }


    @Override
    public void dispose() {
        //listener = null;
    }

    private boolean isReaderConnected() {
        if (reader != null && reader.isConnected())
            return true;
        else {
            if (reader == null)
                 Log.d(TAG, "Reader is null");
            else Log.d(TAG, "Reader is not connected");
            return false;
        }
    }

    @Override
    public void eventReadNotify(RfidReadEvents event) {

        try {
            TagData tag = event.getReadEventData().tagData;
            if(tag.getOpCode() == null || tag.getOpCode()== ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ) {

                TagInfo data = new TagInfo();
                data.epc = tag.getTagID();
                data.antenna = tag.getAntennaID();
                data.rssi = tag.getPeakRSSI();
                data.status = tag.getOpStatus();
                data.size = tag.getTagIDAllocatedSize();
                data.lockData = tag.getPermaLockData();
                if (tag.isContainsLocationInfo()) {
                    data.distance = tag.LocationInfo.getRelativeDistance();
                }
                data.memoryBankData = tag.getMemoryBankData();

                // tracking enabled?
                if (tracking.size() > 0) {
                    if (tracking.contains(data.epc)) {
                        boolean notify = true;
                        if (tags.containsKey(data.epc) && tags.get(data.epc).rssi == data.rssi) notify = false;
                        tags.put(data.epc, data);
                        if (notify) reportTags();
                    }
                }
                else {
                    tags.put(data.epc, data);
                }
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error reading tag data. Error is " + e.toString());
        }
    }

    @Override
    public void eventStatusNotify(RfidStatusEvents event) {

        Log.d(TAG, ">>>>>>>>>>>>>>>>>>> eventStatusNotify()");

        STATUS_EVENT_TYPE eventType = event.StatusEventData.getStatusEventType();

        if (eventType == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {

            HANDHELD_TRIGGER_EVENT_TYPE triggerEvent = event.StatusEventData.HandheldTriggerEventData.getHandheldEvent();

            if (triggerEvent == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED)
            {
                Log.d(TAG, "TRIGGER DOWN");

                new AsyncTasks() {

                    @Override
                    public void onPreExecute() {
                        // before execution
                    }

                    @Override
                    public void doInBackground() {
                        startScanning();
                    }

                    @Override
                    public void onPostExecute() {
                        // Ui task here
                    }

                }.execute();
            }

            else if (triggerEvent == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {

                Log.d(TAG, "TRIGGER UP");

                new AsyncTasks() {

                    @Override
                    public void onPreExecute() {
                        // before execution
                    }

                    @Override
                    public void doInBackground() {
                        stopScanning();
                    }

                    @Override
                    public void onPostExecute() {
                        // Ui task here
                    }

                }.execute();
            }
        }
    }

    synchronized void reportTags() {
        try
        {
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
        catch (Exception e) {
            Log.e(TAG, ">>>>>>>>>>>>>>>>>>>> Error in reportTags()");
        }
    }

    synchronized void startScanning() {

        try
        {
            // clear tracking filter
            tracking.clear();

            if (reader != null) {
                Log.d(TAG, "START SCANNNING");

                // notify listener
                if (listener != null) {
                    listener.notify(INTERFACE, ZebraEvents.startRead,new HashMap<>());
                }

                reader.Actions.Inventory.stop();
                reader.Actions.Inventory.perform();
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, ">>>>>>>>>>>>>>>> Error in startInventory()");
            stopScanning();
        }
    }

    synchronized void stopScanning() {

        // check reader connection
        if (!isReaderConnected()) return;

        try
        {
            if (reader != null) {
                Log.d(TAG, "STOP SCANNING. Found " + tags.size() + " tags");

                // notify listener
                if (listener != null) {
                    listener.notify(INTERFACE, ZebraEvents.stopRead,new HashMap<>());
                }

                // stop the reader
                reader.Actions.Inventory.stop();

                // report tags
                reportTags();
            }
        }
        catch (Exception e) {
            Log.e(TAG, ">>>>>>>>>>>>>>>>>>>> Error in stopInventory()");
        }
    }

    synchronized void startTracking(ArrayList<String> tags) {

        try
        {
            // clear tracking
            tracking.clear();

            if (reader != null) {
                Log.d(TAG, "STARTING TRACKING");

                // notify listener
                if (listener != null) {
                    listener.notify(INTERFACE, ZebraEvents.startRead,new HashMap<>());
                }

                // set tracking tags
                tracking.addAll(tags);

                // stop read
                reader.Actions.Inventory.stop();

                // start inventory
                reader.Actions.Inventory.perform();
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, ">>>>>>>>>>>>>>>> Error in startTracking()");
            stopTracking();
        }
    }

    synchronized void stopTracking() {

        // clear tracking
        tracking.clear();

        // check reader connection
        if (!isReaderConnected()) return;

        try
        {
            if (reader != null) {
                Log.d(TAG, "STOPPING TRACKING. Found " + tags.size() + " tags");

                // notify listener
                if (listener != null) {
                    listener.notify(INTERFACE, ZebraEvents.stopRead,new HashMap<>());
                }

                // stop the reader
                reader.Actions.Inventory.stop();
            }
        }
        catch (Exception e) {
            Log.e(TAG, ">>>>>>>>>>>>>>>>>>>> Error in stopTracking()");
        }
    }

    // configuration
    private void setAntennaPower(int power) {
        Log.d(TAG, "setAntennaPower " + power);
        try {
            // set antenna configurations
            Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
            config.setTransmitPowerIndex(power);
            config.setrfModeTableIndex(0);
            config.setTari(0);
            reader.Config.Antennas.setAntennaRfConfig(1, config);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }

    private void setSingulation(SESSION session, INVENTORY_STATE state) {
        Log.d(TAG, "setSingulation " + session);
        try {
            // Set the singulation control
            Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
            s1_singulationControl.setSession(session);
            s1_singulationControl.Action.setInventoryState(state);
            s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
            reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }

    private void setDPO(boolean bEnable) {
        Log.d(TAG, "setDPO " + bEnable);
        try {
            // control the DPO
            reader.Config.setDPOState(bEnable ? DYNAMIC_POWER_OPTIMIZATION.ENABLE : DYNAMIC_POWER_OPTIMIZATION.DISABLE);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }

    private void setAccessOperationConfiguration() {
        // set required power and profile
        setAntennaPower(240);
        // in case of RFD8500 disable DPO
        if (reader.getHostName().contains("RFD8500"))
            setDPO(false);
        //
        try {
            // set access operation time out value to 1 second, so reader will tries for a second
            // to perform operation before timing out
            reader.Config.setAccessOperationWaitTimeout(1000);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }

    public void write(String epc, String newEpc, String password, String newPassword, String data) {

        if (epc == null || epc.trim().equals("")) return;

        setAccessOperationConfiguration();

        // set default password
        if (password == null || password.trim().length() == 0) password = "0";

        boolean ok = true;

        // write epc
        if (epc != newEpc && newEpc != null && newEpc.trim().length() > 0) {
            Exception exception = writeTag(epc, password, MEMORY_BANK.MEMORY_BANK_EPC, newEpc, 2);
            if (exception != null) {
                ok = false;
                Log.e(TAG, "Error writing tag epc: " + exception.getMessage());
                if (listener != null) {
                    listener.notify(INTERFACE, ZebraEvents.writeFail, ZebraDevice.toError("Error writing tag epc", exception));
                }
            }
            else epc = newEpc;
        }

        // write data
        if (data != null && data.length() > 0) {
            Exception exception = writeTag(epc, password, MEMORY_BANK.MEMORY_BANK_USER, data, 0);
            if (exception != null) {
                ok = false;
                Log.e(TAG, "Error writing tag data: " + exception.getMessage());
                if (listener != null) {
                    listener.notify(INTERFACE, ZebraEvents.writeFail, ZebraDevice.toError("Error writing tag data", exception));
                }
            }
            else data = "";
        }

        // change password
        if (password != newPassword && newPassword != null && newPassword.trim().length() > 0) {
            Exception exception = writeTag(epc, password, MEMORY_BANK.MEMORY_BANK_RESERVED, newPassword, 2);
            if (exception != null) {
                ok = false;
                Log.e(TAG, "Error writing tag password: " + exception.getMessage());
                if (listener != null) {
                    listener.notify(INTERFACE, ZebraEvents.writeFail, ZebraDevice.toError("Error writing tag password", exception));
                }
            }
            else password = newPassword;
        }

        if (listener != null && ok) {
            HashMap<String,Object> hashMap = new HashMap<>();
            TagInfo tag = new TagInfo();
            tag.epc = epc;
            tag.memoryBankData = data;
            tag.password = password;
            hashMap.put("tag",tag);
            listener.notify(INTERFACE, ZebraEvents.writeSuccess, hashMap);
        }
    }


    private Exception writeTag(String sourceEPC, String Password, MEMORY_BANK memory_bank, String targetData, int offset) {
        Log.d(TAG, "WriteTag " + targetData);
        try {
            TagData tagData = null;
            String tagId = sourceEPC;
            TagAccess tagAccess = new TagAccess();
            TagAccess.WriteAccessParams writeAccessParams = tagAccess.new WriteAccessParams();
            String writeData = targetData; //write data in string
            writeAccessParams.setAccessPassword(Long.parseLong(Password,16));
            writeAccessParams.setMemoryBank(memory_bank);
            writeAccessParams.setOffset(offset); // start writing from word offset 0
            writeAccessParams.setWriteData(writeData);
            // set retries in case of partial write happens
            writeAccessParams.setWriteRetries(3);
            // data length in words
            writeAccessParams.setWriteDataLength(writeData.length() / 4);
            // 5th parameter bPrefilter flag is true which means API will apply pre filter internally
            // 6th parameter should be true in case of changing EPC ID it self i.e. source and target both is EPC
            boolean useTIDfilter = memory_bank == MEMORY_BANK.MEMORY_BANK_EPC;
            reader.Actions.TagAccess.writeWait(tagId, writeAccessParams, null, tagData, true, useTIDfilter);

            return null;
        }
        catch (Exception e) {
            e.printStackTrace();
            return e;
        }
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

        public String epc;
        public short antenna;
        public short rssi;
        public ACCESS_OPERATION_STATUS status;
        public short distance;
        public String memoryBankData;
        public String lockData;
        public int size;
        public String seen;
        public String password;

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
