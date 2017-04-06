package com.pixplicity.huethings.network;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.pixplicity.huethings.GsonUtils;
import com.pixplicity.huethings.models.AuthRequest;
import com.pixplicity.huethings.models.AuthResponse;
import com.pixplicity.huethings.models.CapabilitiesResponse;
import com.pixplicity.huethings.models.ErrorResponse;
import com.pixplicity.huethings.models.LightRequest;
import com.pixplicity.huethings.models.LightsResponse;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HueBridge {

    private static final String TAG = HueBridge.class.getSimpleName();

    private static final String DEVICE_TYPE = "hue_things#rpi3";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final String URL = "http://%s/api";
    private static final String URL_CAPABILITIES = "capabilities";
    private static final String URL_LIGHTS_STATE = "lights/%s/state";

    private String mBridgeIp = null; //"10.42.39.194";
    private String mBridgeToken = null; //"vJB9Z1Q-SnW2Lunvzohsn2O17yVq8kqfhsHnNNa2";

    private HttpLoggingInterceptor mOkHttpLogging = new HttpLoggingInterceptor();
    private OkHttpClient mOkHttpClient = new OkHttpClient.Builder()
            .addInterceptor(mOkHttpLogging)
            .build();
    private HueBridgeInterface mHueBridgeInterface;

    private final LinkedList<String> mLights = new LinkedList<>();

    public HueBridge(String bridgeIp) {
        mBridgeIp = bridgeIp;
        mOkHttpLogging.setLevel(HttpLoggingInterceptor.Level.BODY);
    }

    public String getUrl() {
        String url = String.format(Locale.ENGLISH, URL, mBridgeIp);
        if (mBridgeToken != null) {
            url += "/" + mBridgeToken;
        }
        url += "/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mHueBridgeInterface = retrofit.create(HueBridgeInterface.class);
        return url;
    }

    public void setBridgeToken(String bridgeToken) {
        mBridgeToken = bridgeToken;
    }

    public void authenticate(final AuthenticationCallback callback) {
        AuthRequest authRequest = new AuthRequest(DEVICE_TYPE);
        String json = GsonUtils.get().toJson(authRequest);

        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(getUrl())
                .post(body)
                .build();
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
                AuthResponse[] authResponse = GsonUtils.get().fromJson(
                        json, AuthResponse[].class);
                AuthResponse.AuthResponseSuccess success = authResponse[0].success;
                if (success != null) {
                    mBridgeToken = success.username;
                    callback.onSuccess(success);
                } else {
                    ErrorResponse.ResponseError error = authResponse[0].error;
                    callback.onFailure(error, null);
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "request failed", e);
                callback.onFailure(null, e);
            }
        });
    }

    public void capabilities(final HueBridge hueBridge, final CapabilitiesCallback callback) {
        Request request = new Request.Builder()
                .url(getUrl() + URL_CAPABILITIES)
                .get()
                .build();
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
                try {
                    CapabilitiesResponse capabilitiesResponse = GsonUtils.get().fromJson(
                            json, CapabilitiesResponse.class);
                    callback.onSuccess(hueBridge, capabilitiesResponse);
                } catch (IllegalStateException e) {
                    ErrorResponse[] errors = GsonUtils.get().fromJson(
                            json, ErrorResponse[].class);
                    ErrorResponse.ResponseError error = errors[0].error;
                    callback.onFailure(hueBridge, error, null);
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "request failed", e);
                callback.onFailure(hueBridge, null, e);
            }
        });
    }

    public void queryLights(@Nullable
                            final retrofit2.Callback callback) {
        mHueBridgeInterface.lightsList().enqueue(new retrofit2.Callback<LightsResponse>() {
            @Override
            public void onResponse(retrofit2.Call<LightsResponse> call,
                                   retrofit2.Response<LightsResponse> response) {
                mLights.clear();
                LightsResponse lights = response.body();
                for (String lightId : lights.keySet()) {
                    if (lights.get(lightId).state.reachable) {
                        mLights.add(lightId);
                    }
                }
                if (callback != null) {
                    callback.onResponse(call, response);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<LightsResponse> call,
                                  Throwable e) {
                Log.e(TAG, "request failed", e);
                if (callback != null) {
                    callback.onFailure(call, e);
                }
            }
        });
    }

    public void setLights(float hue, float saturation, float brightness, final Callback callback) {
        if (mBridgeIp == null) {
            // We're not configured yet
            return;
        }

        boolean switchedOn = brightness > 0.03;
        int hue2 = Math.round(65000 * hue);
        int saturation2 = Math.round(254 * saturation);
        int brightness2 = Math.round(254 * brightness);

        LightRequest lightRequest = new LightRequest(switchedOn, hue2, saturation2, brightness2);
        String json = GsonUtils.get().toJson(lightRequest);

        for (String lightId : mLights) {
            String urlSuffix = String.format(Locale.ENGLISH, URL_LIGHTS_STATE, lightId);
            doRequest(callback, json, urlSuffix);
        }
    }

    private void doRequest(final Callback callback, String json, String urlSuffix) {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(getUrl() + urlSuffix)
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

    public static class Descriptor {

        @SerializedName("host")
        private final String mHost;

        @SerializedName("bridge_id")
        private final String mBridgeId;

        public Descriptor(String host, String bridgeId) {
            mHost = host;
            mBridgeId = bridgeId;
        }

        public String getHost() {
            return mHost;
        }

        public String getBridgeId() {
            return mBridgeId;
        }

        @Override
        public String toString() {
            return "HueBridge{" + mBridgeId + " at " + mHost + '}';
        }

    }

}
