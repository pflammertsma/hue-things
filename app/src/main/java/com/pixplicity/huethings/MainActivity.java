package com.pixplicity.huethings;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.PeripheralManagerService;
import com.pixplicity.easyprefs.library.Prefs;
import com.pixplicity.huethings.gpio.InputMonitor;
import com.pixplicity.huethings.network.CapabilitiesCallback;
import com.pixplicity.huethings.network.HueBridge;
import com.pixplicity.huethings.network.HueBridgeConnector;
import com.pixplicity.huethings.upnp.UPnPDevice;
import com.pixplicity.huethings.upnp.UPnPDeviceFinder;

import java.io.IOException;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private InputMonitor mInputMonitor = new InputMonitor();
    private HueBridgeConnector mHueBridgeConnector = new HueBridgeConnector(mInputMonitor);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PeripheralManagerService service = new PeripheralManagerService();
        Log.d(TAG, "gpios: " + service.getGpioList());
    }

    @Override
    protected void onStart() {
        super.onStart();

        String bridgeJson = Prefs.getString("last_bridge", null);
        if (bridgeJson != null) {
            HueBridge.Descriptor bridge = GsonUtils.get().fromJson(bridgeJson, HueBridge.Descriptor.class);
            mHueBridgeConnector.connect(bridge.getHost(), bridge.getBridgeId(),
                    new CapabilitiesCallback() {
                        @Override
                        public void onSuccess(HueBridge hueBridge, HueBridge.CapabilitiesResponse success) {
                            // Awesome, we're connected already
                        }

                        @Override
                        public void onFailure(HueBridge hueBridge,
                                              HueBridge.ErrorResponse.ResponseError error, IOException e) {
                            startBridgeScan();
                        }
                    });
        } else {
            startBridgeScan();
        }
    }

    private void startBridgeScan() {
        new UPnPDeviceFinder().observe()
                              .filter(new Func1<UPnPDevice, Boolean>() {
                                  @Override
                                  public Boolean call(UPnPDevice device) {
                                      try {
                                          String bridgeId = device.getProperty("upnp_hue-bridgeid");
                                          if (bridgeId != null) {
                                              device.downloadSpecs();

                                              Log.d(TAG, "Philips Hue bridge discovered: " + device);
                                              mHueBridgeConnector.connectLoop(device.getHost(), bridgeId);
                                              return true;
                                          } else {
                                              Log.d(TAG, "Device discovered: " + device);
                                          }
                                      } catch (Exception e) {
                                          // Ignore errors
                                          Log.e(TAG, "failed obtaining device specs", e);
                                      }
                                      return false;
                                  }
                              })
                              .subscribeOn(Schedulers.io())
                              .observeOn(AndroidSchedulers.mainThread())
                              .toList()
                              .map(new Func1<List<UPnPDevice>, List<UPnPDevice>>() {
                                  @Override
                                  public List<UPnPDevice> call(List<UPnPDevice> devices) {
                                      if (devices.isEmpty()) {
                                          Log.d(TAG, "no UPnP device matches; retrying");
                                          throw new RuntimeException();
                                      }
                                      Log.d(TAG, "UPnP device matches: " + devices.size());
                                      return devices;
                                  }
                              })
                              .retry()
                              .subscribe();
    }

    @Override
    protected void onStop() {
        mHueBridgeConnector.stop();
        mInputMonitor.stop();

        super.onStop();
    }

}