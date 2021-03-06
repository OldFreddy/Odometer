package com.oldfredddy.odometer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private final int PERMISSION_REQUEST_CODE = 698;
    private final int NOTIFICATION_ID = 423;
    private OdometerService odometer;
    private boolean bound = false;

    Button startButton;
    Button pauseButton;
    Button stopButton;

    int indicator = 0; //0 = stop, 1 = start, 2 = pause

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

//        //Обработчики нажатия на кнопку
        startButton = findViewById(R.id.button_start);
        pauseButton = findViewById(R.id.button_pause);
        stopButton = findViewById(R.id.button_stop);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                indicator = 1;
                Toast.makeText(MainActivity.this, "START", Toast.LENGTH_SHORT).show();
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                indicator = 0;
                Toast.makeText(MainActivity.this, String.valueOf(indicator), Toast.LENGTH_SHORT).show();
            }
        });
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                indicator = 2;
                Toast.makeText(MainActivity.this, "PAUSE", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ContextCompat.checkSelfPermission(this, OdometerService.PERMISSION_STRING)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{OdometerService.PERMISSION_STRING},
                    PERMISSION_REQUEST_CODE);
        } else {
            Intent intent = new Intent(this, OdometerService.class);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(this, OdometerService.class);
                    bindService(intent, connection, Context.BIND_AUTO_CREATE);
                } else {
                    //Create notification builder
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                            .setSmallIcon(android.R.drawable.ic_menu_compass)
                            .setContentTitle(getResources().getText(R.string.app_name))
                            .setContentText(getResources().getString(R.string.permission_denied))
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setVibrate(new long[]{1000, 1000})
                            .setAutoCancel(true);
                    //Создание действия
                    Intent actionIntent = new Intent(this, MainActivity.class);
                    PendingIntent actionPendingIntent = PendingIntent.getActivity(this,
                            0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.setContentIntent(actionPendingIntent);

                    //Выдача уведомления
                    NotificationManager notificationManager =
                            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.notify(NOTIFICATION_ID, builder.build());
                }
            }
        }
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

                if ((bound && odometer != null) && indicator == 1) {

                    OdometerService.pauseFlag = true;
                    distance = odometer.getDistance();
                    String splitNumber = splitNum.getText().toString();
                    int splitNumInt = Integer.parseInt(splitNumber);
                    distance = distance / splitNumInt;
                    String distanceStr = String.format(Locale.getDefault(), "%1.3f m", distance);
                    distanceView.setText(distanceStr);
                } else if (indicator == 0) {
                    OdometerService.pauseFlag = false;
                    OdometerService.setDistance(0.0);
                    distanceView.setText("0.0");
                } else if (indicator == 2) {
                    OdometerService.pauseFlag = false;
                    OdometerService.setLastLocation();
                }

                //distanceView.setText((result.setScale(2,RoundingMode.HALF_EVEN)).toString());
                handler.postDelayed(this, 1000);
            }
        });
    }
}