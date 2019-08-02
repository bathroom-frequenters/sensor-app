package me.mahasamatman.isthebathroomavailable;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private SensorManager sensorManager;
    private TextView currentReading;
    private TextView lastSync;
    private boolean currentAvailability;
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // https://stackoverflow.com/a/25093650
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // thanks to https://developer.android.com/guide/topics/sensors/sensors_environment#java
        // since the app needs to keep updating, even when the screen is off,
        // register the listener in #onCreate and do not unregister until the app is killed

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);

        currentReading = findViewById(R.id.currentReading);
        lastSync = findViewById(R.id.lastSync);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        float lux = event.values[0];
        boolean latestAvailability = lux < getResources().getInteger(R.integer.lux_threshold);

        String availability = latestAvailability ? "available" : "unavailable";

        // Update the on screen display
        currentReading.setText("Current Reading: " + lux + " (" + availability + ")");

        // do not need to re-send the same data repeatedly
        if (latestAvailability == currentAvailability) {
            // no state change - return
            return;
        }

        try {
            JSONObject json = new JSONObject()
                    .put("available", latestAvailability)
                    .put("password", getString(R.string.secret));

            RequestBody body = RequestBody.create(JSON, json.toString());
            Request request = new Request.Builder()
                    .url(getString(R.string.api_url))
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();

            String s = response.body().string();

            if (s.equalsIgnoreCase("OK")) {
                Date currentTime = Calendar.getInstance().getTime();

                lastSync.setText("Last Sync: " + currentTime);
                currentAvailability = latestAvailability;
            }
        } catch (IOException e) {
        } catch (JSONException e) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Be sure to unregister the sensor when the activity is destroyed.
        sensorManager.unregisterListener(this);
    }
}
