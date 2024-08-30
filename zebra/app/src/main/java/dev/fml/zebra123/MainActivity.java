package dev.fml.zebra123;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;


public class MainActivity extends AppCompatActivity implements ZebraDeviceListener {

    ZebraDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnConnect).setOnClickListener(v -> {
            boolean isRfid = ZebraRfid.isSupported(this.getApplicationContext());
            if (isRfid)
                 device = new ZebraRfid(getApplicationContext(), this);
            else device = new ZebraDataWedge(getApplicationContext(), this);
            device.connect();
        });
    }

    @Override
    public void notify(final ZebraDevice.ZebraInterfaces source, final ZebraDevice.ZebraEvents event, final HashMap map) {
        map.put("eventSource", source);
        map.put("eventName", event);
    }
}