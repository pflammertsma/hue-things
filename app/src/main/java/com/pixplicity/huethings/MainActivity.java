package com.pixplicity.huethings;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final float MAX_READING = 1023f;
    private static final String URL = "http://10.42.39.194/api/vJB9Z1Q-SnW2Lunvzohsn2O17yVq8kqfhsHnNNa2/lights/3/state";

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    private static final float THRESHOLD = 0.01f;

    private OkHttpClient client = new OkHttpClient();

    private MCP3008 mMCP3008;
    private Handler mHandler;

    private boolean mRequestBusy;
    private float mLastValue = -1;

    private Runnable mReadAdcRunnable = new Runnable() {

        public static final boolean SHOW_ALL_CHANNELS = false;

        private static final long DELAY_MS = 100L;

        @Override
        public void run() {
            if (mMCP3008 == null) {
                return;
            }

            try {
                if (SHOW_ALL_CHANNELS) {
                    Log.d("MCP3008", "ADC 0: " + mMCP3008.readAdc(0x0));
                    Log.d("MCP3008", "ADC 1: " + mMCP3008.readAdc(0x1));
                    Log.d("MCP3008", "ADC 2: " + mMCP3008.readAdc(0x2));
                    Log.d("MCP3008", "ADC 3: " + mMCP3008.readAdc(0x3));
                    Log.d("MCP3008", "ADC 4: " + mMCP3008.readAdc(0x4));
                    Log.d("MCP3008", "ADC 5: " + mMCP3008.readAdc(0x5));
                    Log.d("MCP3008", "ADC 6: " + mMCP3008.readAdc(0x6));
                    Log.d("MCP3008", "ADC 7: " + mMCP3008.readAdc(0x7));
                }
                float val = mMCP3008.readAdc(0x0);
                // Invert and normalize [0-1]
                float normalized = (MAX_READING - val) / MAX_READING;
                Log.d("MCP3008", String.format("value: %f", normalized));
                setLight(normalized);
            } catch (IOException e) {
                Log.d("MCP3008", "Something went wrong while reading from the ADC: " + e.getMessage());
            }

            mHandler.postDelayed(this, DELAY_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // CS (chip select) = BCM22
        // Clock = BCM4
        // DOUT/MISO = BCM27
        // DIN/MOSI = BCM17

        PeripheralManagerService service = new PeripheralManagerService();
        Log.d(TAG, "gpios: " + service.getGpioList());

        try {
            mMCP3008 = new MCP3008("BCM22", "BCM4", "BCM17", "BCM27");
            mMCP3008.register();
        } catch (IOException e) {
            Log.d("MCP3008", "MCP initialization exception occurred: " + e.getMessage());
        }

        mHandler = new Handler();
        mHandler.post(mReadAdcRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mMCP3008 != null) {
            mMCP3008.unregister();
        }

        if (mHandler != null) {
            mHandler.removeCallbacks(mReadAdcRunnable);
        }
    }

    private void setLight(float value) throws IOException {
        if (mRequestBusy) {
            return;
        }
        if (mLastValue >= 0 && Math.abs(mLastValue - value) < THRESHOLD) {
            return;
        }
        mLastValue = value;
        mRequestBusy = true;
        boolean switchedOn = true;
        int saturation = 254;
        int brightness = 254;
//        int brightness = Math.round(254 * value);
        int hue = Math.round(65000 * value);
        String json = "{\n\t\"on\": " + switchedOn + ",\n\t\"sat\": " + saturation + ", \n\t\"bri\": " + brightness + ",\n\t\"hue\": " + hue + "\n}";

        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(URL)
                .put(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "request failed", e);
                mRequestBusy = false;
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, response.body().string());
                mRequestBusy = false;
            }
        });
    }

}