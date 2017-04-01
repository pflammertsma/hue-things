package com.pixplicity.huethings.gpio;

import android.os.Handler;
import android.util.Log;

import com.pixplicity.huethings.network.HueBridge;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class InputMonitor {

    private static final String TAG = InputMonitor.class.getSimpleName();
    private static final boolean VERBOSE = false;

    private static final long SAMPLE_FREQUENCY_MS = 100L;
    private static final long REQUEST_FREQUENCY_MS = 500L;
    private static final float THRESHOLD = 0.01f;
    private static final int SMOOTH_SAMPLING_RATE = 6;
    private static final float MAX_READING = 1023f;
    private static final boolean INVERTED = true;

    // CS (chip select) = BCM22
    // Clock            = BCM4
    // DOUT/MISO        = BCM27
    // DIN/MOSI         = BCM17

    private MCP3008 mMCP3008 = new MCP3008();
    private Handler mHandler = new Handler();

    private Runnable mReadAdcRunnable = new Runnable() {

        public static final boolean SHOW_ALL_CHANNELS = false;

        @Override
        public void run() {
            if (mMCP3008 == null) {
                return;
            }

            try {
                if (SHOW_ALL_CHANNELS) {
                    Log.d(TAG, "ADC 0: " + mMCP3008.readAdc(0x0));
                    Log.d(TAG, "ADC 1: " + mMCP3008.readAdc(0x1));
                    Log.d(TAG, "ADC 2: " + mMCP3008.readAdc(0x2));
                    Log.d(TAG, "ADC 3: " + mMCP3008.readAdc(0x3));
                    Log.d(TAG, "ADC 4: " + mMCP3008.readAdc(0x4));
                    Log.d(TAG, "ADC 5: " + mMCP3008.readAdc(0x5));
                    Log.d(TAG, "ADC 6: " + mMCP3008.readAdc(0x6));
                    Log.d(TAG, "ADC 7: " + mMCP3008.readAdc(0x7));
                }

                float hue = normalize(1);
                if (VERBOSE) {
                    Log.d(TAG, String.format("hue:        %f", hue));
                }
                mHue.add(hue);

                float saturation = normalize(2);
                if (VERBOSE) {
                    Log.d(TAG, String.format("saturation: %f", saturation));
                }
                mSaturation.add(saturation);

                float brightness = normalize(0);
                if (VERBOSE) {
                    Log.d(TAG, String.format("brightness: %f", brightness));
                }
                mBrightness.add(brightness);

                setLight(mHue.getAverage(), mSaturation.getAverage(), mBrightness.getAverage());

            } catch (IOException e) {
                Log.d(TAG, "Something went wrong while reading from the ADC: " + e.getMessage());
            }

            mHandler.postDelayed(this, SAMPLE_FREQUENCY_MS);
        }
    };

    private boolean mRequestBusy;
    private long mRequestTimestamp;
    private float mLastBrightness = -1;
    private float mLastHue = -1;
    private float mLastSaturation = -1;

    private final Rolling mBrightness = new Rolling(SMOOTH_SAMPLING_RATE);
    private final Rolling mHue = new Rolling(SMOOTH_SAMPLING_RATE);
    private final Rolling mSaturation = new Rolling(SMOOTH_SAMPLING_RATE);

    private HueBridge mHueBridge;

    public void start() {
        try {
            mMCP3008.register("BCM22", "BCM4", "BCM17", "BCM27");
            mHandler.post(mReadAdcRunnable);
        } catch (IOException e) {
            Log.e(TAG, "MCP initialization error", e);
        }
    }

    public boolean isStarted() {
        return mMCP3008.isRegistered();
    }

    public void stop() {
        if (mMCP3008 != null) {
            mMCP3008.unregister();
        }

        if (mHandler != null) {
            mHandler.removeCallbacks(mReadAdcRunnable);
        }
    }

    public void setHueBridge(HueBridge hueBridge) {
        mHueBridge = hueBridge;
    }

    private float normalize(int channel) throws IOException {
        float val = mMCP3008.readAdc(channel);
        if (INVERTED) {
            val = (MAX_READING - val);
        }
        // Normalize between [0-1]
        return val / MAX_READING;
    }

    private void setLight(float hue, float saturation, float brightness) throws IOException {
        if (mHueBridge == null) {
            // Not ready yet
            return;
        }
        Log.d("MCP3008",
              String.format(Locale.ENGLISH, "\thue: %.3f \tsat: %.3f \tbri: %.3f",
                            hue, saturation, brightness));
        if (mRequestBusy || mRequestTimestamp > System.currentTimeMillis() - REQUEST_FREQUENCY_MS) {
            return;
        }
        if (mLastBrightness >= 0 && Math.abs(mLastBrightness - brightness) < THRESHOLD &&
                mLastHue >= 0 && Math.abs(mLastHue - hue) < THRESHOLD &&
                mLastSaturation >= 0 && Math.abs(mLastSaturation - saturation) < THRESHOLD) {
            return;
        }

        mLastBrightness = brightness;
        mLastHue = hue;
        mLastSaturation = saturation;
        mRequestBusy = true;
        mRequestTimestamp = System.currentTimeMillis();

        mHueBridge.setLights(hue, saturation, brightness,
                             new Callback() {
                                      @Override
                                      public void onFailure(Call call, IOException e) {
                                          mRequestBusy = false;
                                      }

                                      @Override
                                      public void onResponse(Call call, Response response) throws IOException {
                                          mRequestBusy = false;
                                      }
                                  });
    }

    public class Rolling {

        private int size;
        private int fill;
        private float total = 0f;
        private int index = 0;
        private float samples[];

        public Rolling(int size) {
            this.size = size;
            samples = new float[size];
        }

        public void add(float x) {
            total -= samples[index];
            samples[index] = x;
            total += x;
            if (fill < size) {
                fill = index + 1;
            }
            if (++index == size) {
                index = 0; // cheaper than modulus
            }
        }

        public float getAverage() {
            if (fill == 0) {
                return 0f;
            }
            return total / fill;
        }

    }

}
