package com.pixplicity.huethings.network;

import android.util.Log;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class HueManager {

    private static final String TAG = HueManager.class.getSimpleName();

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final String URL = "http://%s/api";
    private static final String URL_LIGHTS = "/lights/%d/state";

    private String mBridgeIp = null; //"10.42.39.194";
    private String mBridgeToken = null; //"vJB9Z1Q-SnW2Lunvzohsn2O17yVq8kqfhsHnNNa2";
    private int mLightId = 0;

    private HttpLoggingInterceptor mOkHttpLogging = new HttpLoggingInterceptor();
    private OkHttpClient mOkHttpClient = new OkHttpClient.Builder()
            .addInterceptor(mOkHttpLogging)
            .build();

    public HueManager() {
        mOkHttpLogging.setLevel(HttpLoggingInterceptor.Level.BODY);
    }

    public void setLights(float hue, float saturation, float brightness, final Callback callback) {
        if (mBridgeIp == null) {
            // We're not configured yet
            return;
        }

        boolean switchedOn = true;
        int hue2 = Math.round(65000 * hue);
        int saturation2 = Math.round(254 * saturation);
        int brightness2 = Math.round(254 * brightness);

        String json = "{\n\t\"on\": " + switchedOn + ",\n\t\"sat\": " + saturation2 + ", \n\t\"bri\": " + brightness2 + ", \n\t\"hue\": " + hue2 + "\n}";
        //Log.d("MCP3008", String.format("json: %s", json));

        String url = String.format(Locale.ENGLISH, URL, mBridgeIp, mBridgeToken, mLightId);

        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, response.body().string());
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "request failed", e);
                callback.onFailure(call, e);
            }
        });
    }

}
