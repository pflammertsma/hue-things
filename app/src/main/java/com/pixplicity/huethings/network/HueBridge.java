package com.pixplicity.huethings.network;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.pixplicity.huethings.listeners.OnLightsUpdated;
import com.pixplicity.huethings.models.AuthRequest;
import com.pixplicity.huethings.models.AuthResponse;
import com.pixplicity.huethings.models.CapabilitiesResponse;
import com.pixplicity.huethings.models.LightRequest;
import com.pixplicity.huethings.models.LightResponse;
import com.pixplicity.huethings.models.LightsResponse;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HueBridge {

    private static final String TAG = HueBridge.class.getSimpleName();

    private static final String DEVICE_TYPE = "hue_things#rpi3";

    private static final String URL = "http://%s/api";

    private String mBridgeIp = null;
    private String mBridgeToken = null;

    private HttpLoggingInterceptor mOkHttpLogging = new HttpLoggingInterceptor();
    private OkHttpClient mOkHttpClient = new OkHttpClient.Builder()
            .addInterceptor(mOkHttpLogging)
            .build();
    private HueBridgeInterface mHueBridgeInterface;

    private final LinkedList<String> mLights = new LinkedList<>();

    public HueBridge(String bridgeIp) {
        mBridgeIp = bridgeIp;
        mOkHttpLogging.setLevel(HttpLoggingInterceptor.Level.BODY);
        createInterface();
    }

    private void createInterface() {
        String url = String.format(Locale.ENGLISH, URL, mBridgeIp);
        if (mBridgeToken != null) {
            url += "/" + mBridgeToken;
        }
        url += "/";
        Retrofit retrofit = new Retrofit.Builder()
                .client(mOkHttpClient)
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mHueBridgeInterface = retrofit.create(HueBridgeInterface.class);
    }

    public void setBridgeToken(String bridgeToken) {
        mBridgeToken = bridgeToken;
        createInterface();
    }

    public void authenticate(final AuthenticationCallback callback) {
        AuthRequest authRequest = new AuthRequest(DEVICE_TYPE);
        mHueBridgeInterface.authenticate(authRequest).enqueue(new retrofit2.Callback<AuthResponse[]>() {
            @Override
            public void onResponse(retrofit2.Call<AuthResponse[]> call,
                                   retrofit2.Response<AuthResponse[]> response) {
                AuthResponse.AuthResponseSuccess success = response.body()[0].success;
                if (callback != null) {
                    callback.onSuccess(success);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<AuthResponse[]> call, Throwable e) {
                Log.e(TAG, "request failed", e);
                callback.onFailure(null, e);
            }
        });
    }

    public void capabilities(final HueBridge hueBridge,
                             @Nullable
                             final CapabilitiesCallback callback) {
        mHueBridgeInterface.capabilities().enqueue(new retrofit2.Callback<CapabilitiesResponse>() {
            @Override
            public void onResponse(retrofit2.Call<CapabilitiesResponse> call,
                                   retrofit2.Response<CapabilitiesResponse> response) {
                if (callback != null) {
                    callback.onSuccess(hueBridge, response.body());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<CapabilitiesResponse> call, Throwable e) {
                Log.e(TAG, "request failed", e);
                if (callback != null) {
                    callback.onFailure(hueBridge, null, e);
                }
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
                    LightResponse light = lights.get(lightId);
                    if (light.state.reachable) {
                        Log.d(TAG, "found light: " + light.name + "; " + light.state);
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

    public void setLights(float hue, float saturation, float brightness,
                          final OnLightsUpdated callback) {
        if (mBridgeIp == null) {
            // We're not configured yet
            return;
        }

        boolean switchedOn = brightness > 0.03;
        Integer hue2 = null;
        Integer saturation2 = null;
        Integer brightness2 = null;
        if (switchedOn) {
            hue2 = Math.round(65000 * hue);
            saturation2 = Math.round(254 * saturation);
            brightness2 = Math.round(254 * brightness);
        }

        LightRequest lightRequest = new LightRequest(switchedOn, hue2, saturation2, brightness2);

        final Semaphore semaphore = new Semaphore(-mLights.size() + 1);

        for (final String lightId : mLights) {
            mHueBridgeInterface.lightUpdate(lightId, lightRequest).enqueue(new retrofit2.Callback<Void>() {
                @Override
                public void onResponse(retrofit2.Call<Void> call,
                                       retrofit2.Response<Void> response) {
                    if (callback != null) {
                        callback.onLightUpdated(lightId);
                    }
                    semaphore.release();
                }

                @Override
                public void onFailure(retrofit2.Call<Void> call, Throwable e) {
                    Log.e(TAG, "request failed", e);
                    if (callback != null) {
                        callback.onLightUpdateFailed(lightId, e);
                    }
                    semaphore.release();
                }
            });
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    semaphore.acquire();
                } catch (InterruptedException ignore) {
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void ignore) {
                callback.onLightsUpdated(mLights.size());
            }
        }.execute();
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
