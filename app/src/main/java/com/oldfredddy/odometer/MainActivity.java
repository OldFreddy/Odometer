package com.oldfredddy.odometer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private OdometerService odometer;
    private boolean bound = false;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            OdometerService.OdometerBinder odometerBinder = (OdometerService.OdometerBinder) binder;
            odometer = odometerBinder.getOdometer();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        displayDistance();

    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, OdometerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }

    private void displayDistance() {
        final TextView distanceView = findViewById(R.id.distance);
        final TextView splitNum = findViewById(R.id.split);
        final Handler handler = new Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {
                double distance = 0.0;
                Location startLocation = null;

                if (bound && odometer != null) {
                    distance = odometer.getDistance();
                    String splitNumber = splitNum.getText().toString();
                    int splitNumInt = Integer.parseInt(splitNumber);
                    distance = distance/splitNumInt;
                }
                String distanceStr = String.format(Locale.getDefault(), "%1.3f m", distance);
                distanceView.setText(distanceStr);
                //distanceView.setText((result.setScale(2,RoundingMode.HALF_EVEN)).toString());
                handler.postDelayed(this, 1000);
            }
        });
    }
}