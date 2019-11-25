package com.nqnghia.chartthingspeakwithmqtt;

import android.app.DownloadManager;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    // Parameter ThingSpeak
    private static final String TAG = "UsingThingSpeakAPI";
    private static final String THINGSPEAK_CHANNEL_ID = "866855";
    private static final String THINGSPEAK_WRITE_API_KEY = "R8MCYSHH20RIBJWV";
    private static final String THINGSPEAK_READ_API_KEY = "QTRKZPHW65T7IHP2";
    private static final String THINGSPEAK_MQTT_API_KEY = "5YSL39EJ2T6DQBDP";

    private static final String THINGSPEAK_SERVER = "mqtt.thingspeak.com";
    private static final int THINGSPEAK_PORT = 1883;
    private static final String SERVER_URL = "tcp://" + THINGSPEAK_SERVER + ":" + THINGSPEAK_PORT;
    private static final String THINGSPEAK_SUBSCRIBE_FIELD1 = "channels/" + THINGSPEAK_CHANNEL_ID +
            "/subscribe/fields/field1/" + THINGSPEAK_READ_API_KEY;
    private static final String THINGSPEAK_SUBSCRIBE_FIELD2 = "channels/" + THINGSPEAK_CHANNEL_ID +
            "/subscribe/fields/field2/" + THINGSPEAK_READ_API_KEY;
    private static final String THINGSPEAK_SUBSCRIBE_FIELDS = "channels/" + THINGSPEAK_CHANNEL_ID +
            "/subscribe/fields/+/" + THINGSPEAK_READ_API_KEY;
    private static final String THINGSPEAK_PUBLISH_FIELD1 = "channels/" + THINGSPEAK_CHANNEL_ID +
            "/publish/fields/field1/" + THINGSPEAK_WRITE_API_KEY;
    private static final String THINGSPEAK_PUBLISH_FIELD2 = "channels/" + THINGSPEAK_CHANNEL_ID +
            "/publish/fields/field2/" + THINGSPEAK_WRITE_API_KEY;
    private static final String THINGSPEAK_PUBLISH_FIELDS = "channels/" + THINGSPEAK_CHANNEL_ID +
            "/publish/" + THINGSPEAK_WRITE_API_KEY;

    private static final String THINGSPEAK_FEEDS = "/feeds.json?api_key=";
    private static final String THINGSPEAK_FIEDS = "/fields/1.json?api_key=";
    private static final String THINGSPEAK_FIELD1 = "&field1=";
    private static final String THINGSPEAK_FIELD2 = "&field2=";
    private static final String THINGSPEAK_RESULTS = "&results=";
    private static final String THINGSPEAK_INIT_RESULTS_SIZE = "75";
    private static final String THINGSPEAK_RESULTS_SIZE = "1";
    private static final String THINGSPEAK_UPDATE_URL = "https://api.thingspeak.com/update?api_key=";
    private static final String THINGSPEAK_CHANNEL_URL = "https://api.thingspeak.com/channels/";

    private static final int MAX_DATA_POINTS = 80;
    private static final int GET_TIME = 4000;

    private TextView textView;
    private GraphView graph;
    private BarGraphSeries<DataPoint> series1;
    private LineGraphSeries<DataPoint> series2;

    private Random random;

    // MQTT
    private MqttHelper mqttHelper;
    private static final int QoS0 = 0;
    private static Boolean StartedFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        random = new Random();

        setupGraph();
    }

    @Override
    protected void onStart() {
        super.onStart();

        StartedFlag = false;
        mqttHelper = new MqttHelper(getApplicationContext(), THINGSPEAK_SERVER, THINGSPEAK_PORT, MqttClient.generateClientId(), THINGSPEAK_MQTT_API_KEY);
        mqttHelper.connect();
        mqttHelper.setMqttHandler(new MqttHelper.MqttHandler() {
            @Override
            public void handle(String topic, final MqttMessage message) {
                if (StartedFlag) {
                    encoding(topic, message);
                } else {
                    StartedFlag = true;
                }
            }
        });
        mqttHelper.setMqttSubscribe(new MqttHelper.MqttSubscribe() {
            @Override
            public void setSubscribe(IMqttToken asyncActionToken) {
                mqttHelper.subscribe(THINGSPEAK_SUBSCRIBE_FIELDS, QoS0);
            }
        });
    }

    private void encoding(String topic, final MqttMessage message) {
        if (topic.contains("field1")) {
            Log.d("field1", message.toString());
            series1.appendData(new DataPoint(series1.getHighestValueX() + 1,
                            Integer.parseInt(message.toString())),
                    true, MAX_DATA_POINTS);
        } else if (topic.contains("field2")) {
            Log.d("field2", message.toString());
            series2.appendData(new DataPoint(series1.getHighestValueX() + 1,
                            Integer.parseInt(message.toString())),
                    true, MAX_DATA_POINTS);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setTextColor(Color.rgb(
                        random.nextInt(255),
                        random.nextInt(255),
                        random.nextInt(255)));
                textView.setText("Updated Chart");
            }
        });
    }

    public void setupGraph() {
        textView = findViewById(R.id.textView);

        graph = findViewById(R.id.graph);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setScalable(true);
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setTextColor(Color.rgb(0, 255, 255));
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        graph.getLegendRenderer().setMargin(40);
        graph.getLegendRenderer().setPadding(10);
        graph.getViewport().setMaxY(80);
        graph.getViewport().setMinY(0);
        graph.setTitle("Field 1 & Field 2");
        graph.setTitleColor(Color.BLUE);
        graph.setBackgroundColor(Color.rgb(255, 165, 0));
        graph.getGridLabelRenderer().setHorizontalAxisTitle("X");
        graph.getGridLabelRenderer().setVerticalAxisTitle("Y");
        graph.getGridLabelRenderer().setHorizontalAxisTitleColor(Color.BLUE);
        graph.getGridLabelRenderer().setVerticalAxisTitleColor(Color.GREEN);

        series1 = new BarGraphSeries<>();
        series1.setTitle("Field 1");
        series1.setDataWidth(0.5);
        series1.setColor(Color.BLUE);
        graph.addSeries(series1);

        series2 = new LineGraphSeries<>();
        series2.setTitle("Field 2");
        series2.setDrawDataPoints(true);
        series2.setDataPointsRadius(10);
        series2.setThickness(1);
        series2.setColor(Color.GREEN);
        graph.addSeries(series2);

        setupChart();
    }

    private void setupChart() {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        Request request = builder.url(THINGSPEAK_CHANNEL_URL +
                THINGSPEAK_CHANNEL_ID +
                THINGSPEAK_FIEDS +
                THINGSPEAK_READ_API_KEY +
                THINGSPEAK_RESULTS +
                THINGSPEAK_INIT_RESULTS_SIZE).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.d("Request to receive message", "Failure");
            }

            @Override
            public void onResponse(Response response) throws IOException {
                final String jsonString = response.body().string();
                Log.d("Response", jsonString);
                initJsonAnalyze(jsonString);
            }
        });
    }

    private void initJsonAnalyze(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);
            final JSONArray jsonArray = jsonObject.getJSONArray("feeds");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        textView.setTextColor(Color.rgb(
                                random.nextInt(255),
                                random.nextInt(255),
                                random.nextInt(255)));

                        for(int i = 0; i < jsonArray.length(); i++) {
                            series1.appendData(new DataPoint(i,
                                            Integer.parseInt(jsonArray.getJSONObject(i).getString("field1"))),
                                    true, MAX_DATA_POINTS);
                            series2.appendData(new DataPoint(i,
                                            Integer.parseInt(jsonArray.getJSONObject(i).getString("field2"))),
                                    true, MAX_DATA_POINTS);
                        }

                        textView.setText("Updated data.");
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
