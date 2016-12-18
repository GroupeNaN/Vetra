package fr.esiea.et.vetra;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/*************************************************************
 **															**
 **			____   ____      __                 			**
 **			\   \ /   /_____/  |_____________   			**
 **			 \   Y   // __ \   __\_  __ \__  \  			**
 **			  \     /\  ___/|  |  |  | \// __ \_			**
 **		 	   \___/  \___  >__|  |__|  (____  /			**
 **			              \/                 \/ 			**
 **															**
 **															**
 **************************************************************/

public class SeriesCore extends AppCompatActivity implements SensorEventListener
{
    private class SeriesCoreBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(intent.getAction().equals(VetraCoreData.INTENT_DL_OVER))
            {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

                builder.setSmallIcon(R.drawable.ic_download_24dp);
                builder.setContentTitle(getResources().getString(R.string.notif_refresh_over_title));
                builder.setContentText(getResources().getString(R.string.notif_refresh_over_content));

                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(1, builder.build());
            }
        }
    }

    //Shake detection
    private SensorManager sensorManager;
    private Sensor accelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //Register for notifications sent by the download thread
        LocalBroadcastManager.getInstance(this).registerReceiver(new SeriesCoreBroadcastReceiver(), new IntentFilter(VetraCoreData.INTENT_DL_OVER));

        //Initialize VetraCoreData so the static context is registered
        //getDatabasePath(VetraCoreData.DB_FILE).delete();
        new VetraCoreData(this);

        //Register for motions
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //UI setup
        setContentView(R.layout.activity_series_core);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new VetraFragmentPagerAdapter(getSupportFragmentManager(), this));

        // Give the TabLayout to the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(38, 76, 210)));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(SeriesCore.this, VetraSearchActivity.class);
                intent.setAction(Intent.ACTION_SEARCH);
                SeriesCore.this.startActivity(intent);
                //onSearchRequested();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_series_core, menu);
        return true;
    }

    //Menu button management
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh)
        {
            Toast.makeText(this, this.getResources().getString(R.string.start_refresh), Toast.LENGTH_SHORT).show();
            new VetraCoreData(this).updateData();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Shaking
    @Override
    public void onResume()
    {
        super.onResume();
        sensorManager.registerListener(this, accelerometer,	SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause()
    {
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {} //¯\_(ツ)_/¯

    private long lastShake = 0;
    private boolean alertRunning = false;
    public void onSensorChanged(SensorEvent event)
    {
        float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
        float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
        float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;

        // gForce will be close to 1 when there is no movement.
        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        //Threshold to register
        if (gForce > 2.6 && !alertRunning)
        {
            final long now = System.currentTimeMillis();
            //You have to wait 1 second between each shaking
            if(now - lastShake < 1000)
                return;

            lastShake = now;

            //Show the easter egg
            int messageID = R.string.easter_egg_1;
            switch ((int) (Math.round(Math.random() * 10) % 3))
            {
                case 0:
                {
                    messageID = R.string.easter_egg_2;
                    break;
                }

                case 1:
                {
                    messageID = R.string.easter_egg_3;
                    break;
                }
            }

            alertRunning = true;

            //Show the alert dialog
            new AlertDialog.Builder(this)
                    .setMessage(getResources().getString(messageID))
                    .setPositiveButton("Ok...", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            alertRunning = false;
                        }
                    })
                    .create().show();
        }
    }
}
