package com.pixplicity.huethings.network;

import android.os.Handler;

import com.pixplicity.easyprefs.library.Prefs;
import com.pixplicity.huethings.gpio.InputMonitor;

import java.io.IOException;
import java.util.LinkedList;

public class HueBridgeConnector {

    private InputMonitor mInputMonitor;

    private Handler mHandler = new Handler();
    private LinkedList<Runnable> mRunnables = new LinkedList<>();

    public HueBridgeConnector(InputMonitor inputMonitor) {
        mInputMonitor = inputMonitor;
    }

    public void connect(String host, final String bridgeId) {
        final HueBridge hueBridge = new HueBridge(host);
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final Runnable runnable = this;
                AuthenticationCallback callback = new AuthenticationCallback() {
                    @Override
                    public void onSuccess(HueBridge.AuthResponse.AuthResponseSuccess success) {
                        // Store the token for later use
                        String bridgeToken = success.username;
                        Prefs.putString("bridge_" + bridgeId, bridgeToken);

                        onConnected(bridgeToken, hueBridge);
                    }

                    @Override
                    public void onFailure(HueBridge.HueResponse.ResponseError error, IOException e) {
                        mHandler.postDelayed(runnable, 5000);
                    }
                };
                hueBridge.authenticate(callback);
            }
        };
        final String bridgeToken = Prefs.getString("bridge_" + bridgeId, null);
        if (bridgeToken != null) {
            // Check if our session is valid
            hueBridge.capabilities(new CapabilitiesCallback() {
                @Override
                public void onSuccess(HueBridge.CapabilitiesResponse success) {
                    onConnected(bridgeToken, hueBridge);
                }

                @Override
                public void onFailure(HueBridge.HueResponse.ResponseError error, IOException e) {
                    // Forget this token as it's evidently no longer valid
                    Prefs.remove("bridge_" + bridgeId);

                    // Proceed with attempting to connect
                    mRunnables.add(runnable);
                    mHandler.post(runnable);
                }
            });
        } else {
            mRunnables.add(runnable);
            mHandler.post(runnable);
        }
    }

    public void stop() {
        for (Runnable runnable : mRunnables) {
            mHandler.removeCallbacks(runnable);
        }
    }

    public void onConnected(String bridgeToken, HueBridge hueBridge) {
        // Stop discovery now that we're connected
        stop();

        // Finish configuring the HueBridge
        hueBridge.setBridgeToken(bridgeToken);

        // Configure and start the InputMonitor
        mInputMonitor.setHueBridge(hueBridge);
        if (!mInputMonitor.isStarted()) {
            mInputMonitor.start();
        }

    }

}
