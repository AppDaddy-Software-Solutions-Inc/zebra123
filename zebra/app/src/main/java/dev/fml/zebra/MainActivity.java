package dev.fml.zebra123;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnConnect).setOnClickListener(v -> {
            Zebra z = new Zebra(getApplicationContext());
            z.connect();
        });
    }
}