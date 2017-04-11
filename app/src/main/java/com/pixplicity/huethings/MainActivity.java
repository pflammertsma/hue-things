package com.pixplicity.huethings;

import android.app.Activity;
import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventCallback;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.PeripheralManagerService;
import com.pixplicity.easyprefs.library.Prefs;
import com.pixplicity.huethings.dht.Dhtxx;
import com.pixplicity.huethings.dht.DhtxxHumidityAndTemperatureDriver;
import com.pixplicity.huethings.gpio.InputMonitor;
import com.pixplicity.huethings.models.CapabilitiesResponse;
import com.pixplicity.huethings.models.ErrorResponse;
import com.pixplicity.huethings.network.CapabilitiesCallback;
import com.pixplicity.huethings.network.HueBridge;
import com.pixplicity.huethings.network.HueBridgeConnector;
import com.pixplicity.huethings.sensors.SensorCallback;
import com.pixplicity.huethings.upnp.UPnPDevice;
import com.pixplicity.huethings.upnp.UPnPDeviceFinder;

import java.io.IOException;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String DHT_PIN = "BCM18";

    private static final boolean ENABLE_HUE = false;
    private static final boolean ENABLE_DHT = true;

    private InputMonitor mInputMonitor = new InputMonitor();
    private HueBridgeConnector mHueBridgeConnector = new HueBridgeConnector(mInputMonitor);

    private SensorManager mSensorManager;
    private DhtxxHumidityAndTemperatureDriver mDhtSensor;
    private SensorCallback mDhtSensorCallback;

    // FIXME temporary manual polling
    private Handler mHandler = new Handler();
    private Runnable mDhtRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PeripheralManagerService service = new PeripheralManagerService();
        Log.d(TAG, "gpios: " + service.getGpioList());

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ENABLE_HUE) {
            String bridgeJson = Prefs.getString("last_bridge", null);
            if (bridgeJson != null) {
                HueBridge.Descriptor bridge = GsonUtils.get().fromJson(bridgeJson, HueBridge.Descriptor.class);
                Log.d(TAG, "reconnecting to previous bridge " + bridge);
                mHueBridgeConnector.connect(bridge.getHost(), bridge.getBridgeId(),
                        new CapabilitiesCallback() {
                            @Override
                            public void onSuccess(HueBridge hueBridge,
                                                  CapabilitiesResponse success) {
                                // Awesome, we're connected already
                            }

                            @Override
                            public void onFailure(HueBridge hueBridge,
                                                  ErrorResponse.ResponseError error, Throwable e) {
                                startBridgeScan();
                            }
                        });
            } else {
                startBridgeScan();
            }
        }

        if (ENABLE_DHT) {
            if (mDhtSensor == null) {
                try {
                    mDhtSensor = new DhtxxHumidityAndTemperatureDriver(DHT_PIN, Dhtxx.DHT22_TYPE);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                mDhtSensorCallback = new SensorCallback(mSensorManager,
                        new SensorEventCallback() {
                            @Override
                            public void onSensorChanged(SensorEvent event) {
                                Log.d(TAG, "onSensorChanged: " + event);
                            }
                        });
            }
            if (false) {
                mDhtSensor.registerTemperatureSensor();
                mSensorManager.registerDynamicSensorCallback(mDhtSensorCallback);
            }
            mDhtRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        float temp = mDhtSensor.readTemperature();
                        Log.d(TAG, "temperature: " + temp);
                    } catch (IOException e) {
                        Log.e(TAG, "failed reading temperature", e);
                    }
                    mHandler.postDelayed(this, 10000);
                }
            };
            mHandler.post(mDhtRunnable);
        }
    }

    @Override
    protected void onStop() {
        if (ENABLE_DHT) {
            mSensorManager.unregisterDynamicSensorCallback(mDhtSensorCallback);
            mDhtSensor.unregisterTemperatureSensor();
        }
        if (ENABLE_HUE) {
            mHueBridgeConnector.stop();
            mInputMonitor.stop();
        }

        super.onStop();
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

}