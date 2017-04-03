package com.pixplicity.huethings.network;

import android.os.Handler;

import com.pixplicity.easyprefs.library.Prefs;
import com.pixplicity.huethings.GsonUtils;
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

    public void connectLoop(final String host, final String bridgeId) {
        connect(host, bridgeId, new CapabilitiesCallback() {
            @Override
            public void onSuccess(HueBridge hueBridge, HueBridge.CapabilitiesResponse success) {
            }

            @Override
            public void onFailure(final HueBridge hueBridge,
                                  HueBridge.ErrorResponse.ResponseError error, IOException e) {
                // Proceed with attempting to connectLoop
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        final Runnable runnable = this;
                        AuthenticationCallback callback = new AuthenticationCallback() {
                            @Override
                            public void onSuccess(HueBridge.AuthResponse.AuthResponseSuccess success) {
                                // Store the token for later use
                                String bridgeToken = success.username;
                                String bridge = GsonUtils.get().toJson(new HueBridge.Descriptor(host, bridgeId));
                                Prefs.putString("last_bridge", bridge);
                                Prefs.putString("bridge_" + bridgeId, bridgeToken);

                                onConnected(bridgeToken, hueBridge);
                            }

                            @Override
                            public void onFailure(HueBridge.ErrorResponse.ResponseError error, IOException e) {
                                mHandler.postDelayed(runnable, 5000);
                            }
                        };

                        // Reset the HueBridge to authenticate
                        hueBridge.setBridgeToken(null);
                        hueBridge.authenticate(callback);
                    }
                };

                mRunnables.add(runnable);
                mHandler.post(runnable);
            }
        });
    }

    public void connect(final String host, final String bridgeId, final CapabilitiesCallback callback) {
        final HueBridge hueBridge = new HueBridge(host);

        final String bridgeToken = Prefs.getString("bridge_" + bridgeId, null);
        if (bridgeToken != null) {
            hueBridge.setBridgeToken(bridgeToken);
            // Check if our session is valid
            hueBridge.capabilities(hueBridge, new CapabilitiesCallback() {
                @Override
                public void onSuccess(HueBridge hueBridge, HueBridge.CapabilitiesResponse success) {
                    onConnected(bridgeToken, hueBridge);
                    if (callback != null) {
                        callback.onSuccess(hueBridge, success);
                    }
                }

                @Override
                public void onFailure(HueBridge hueBridge, HueBridge.ErrorResponse.ResponseError error, IOException e) {
                    // Forget this token as it's evidently no longer valid
                    Prefs.remove("bridge_" + bridgeId);
                    if (callback != null) {
                        callback.onFailure(hueBridge, error, e);
                    }
                }
            });
        } else if (callback != null) {
            callback.onFailure(hueBridge, null, null);
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
