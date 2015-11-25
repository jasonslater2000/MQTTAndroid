package com.jason.mqtt.gingerbread;

import android.app.Activity;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.joda.time.DateTime;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by jason on 11/25/15.
 */
public class LocationHandler implements LocationListener {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String topic = "snaplogic/device";
    private static final int qos = 2;
    private static final String broker = "tcp://iot.eclipse.org:1883";
    private static final MemoryPersistence persistence = new MemoryPersistence();
    private static final String TAG = "LocationHandler";
    private LocationManager locationManager;
    private MqttClient sampleClient;
    private Activity context;

    public LocationHandler(Activity context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
        try {
            sampleClient = new MqttClient(broker, MqttClient.generateClientId(), persistence);
        } catch (MqttException e) {
            displayText(e.toString());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Map<String,Object> map = locationToMap(location);
        displayText(mapToJson(map,true));
        publishMessage(mapToJson(map, false));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void pause() {
        Log.i(TAG, "pause");
        try {
            sampleClient.disconnect();
            locationManager.removeUpdates(this);
        } catch (MqttException|SecurityException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        Log.i(TAG, "resume");
        try {
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            sampleClient.connect(connOpts);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        } catch (MqttException|SecurityException e) {
            displayText(e.toString());
        }
    }

    protected void publishMessage(String text) {
        try {
            MqttMessage message = new MqttMessage(text.getBytes());
            message.setQos(qos);
            sampleClient.publish(topic, message);
        } catch (MqttException e) {
            displayText(e.toString());
        }
    }

    protected Map<String,Object> locationToMap(Location location) {
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("dir", String.valueOf(location.getBearing()));
        map.put("ts", DateTime.now().toString());
        Map<String, Object> loc = new LinkedHashMap<>();
        loc.put("lat", String.valueOf(location.getLatitude()));
        loc.put("long", String.valueOf(location.getLongitude()));
        map.put("loc", loc);
        String android_id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        map.put("client", android_id);
        return map;
    }

    private String mapToJson( Map<String,Object> map, boolean pretty ) {
        String text = "";
        ObjectWriter writer = pretty ? mapper.writerWithDefaultPrettyPrinter() : mapper.writer();
        try {
            text = writer.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return text;
    }

    protected void displayText(String text) {
        TextView tv = (TextView) context.findViewById(R.id.message);
        tv.setText(text);
    }
}
