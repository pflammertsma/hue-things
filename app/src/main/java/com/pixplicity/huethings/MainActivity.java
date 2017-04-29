package com.pixplicity.huethings;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.pixplicity.easyprefs.library.Prefs;
import com.pixplicity.huethings.gpio.InputMonitor;
import com.pixplicity.huethings.models.CapabilitiesResponse;
import com.pixplicity.huethings.models.ErrorResponse;
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

    private static final boolean BRIDGE_SCAN_ENABLED = false;

    private static final String BUTTON1_PIN = "BCM5";
    private static final String BUTTON2_PIN = "BCM6";
    private static final String BUTTON3_PIN = "BCM19";
    private static final String BUTTON4_PIN = "BCM26";
    private static final String BUTTON5_PIN = "BCM16";
    private static final String LED1_PIN = "BCM18";
    private static final String LED2_PIN = "BCM23";
    private static final String LED3_PIN = "BCM24";
    private static final String LED4_PIN = "BCM25";
    private static final String LED5_PIN = "BCM12";

    private PeripheralManagerService mPioService;
    private InputMonitor mInputMonitor = new InputMonitor();
    private HueBridgeConnector mHueBridgeConnector = new HueBridgeConnector(mInputMonitor);
    private ButtonInputDriver mButton1, mButton2, mButton3, mButton4, mButton5;
    private Gpio mLed1, mLed2, mLed3, mLed4, mLed5;
    private boolean mLed1On, mLed2On, mLed3On, mLed4On, mLed5On;

    private final Handler mHandler = new Handler();

    private TextView mTvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTvInfo = (TextView) findViewById(R.id.tv_info);

        mPioService = new PeripheralManagerService();
        debug("gpios: " + mPioService.getGpioList());
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Configure buttons
        try {
            mButton1 = new ButtonInputDriver(
                    BUTTON1_PIN,
                    Button.LogicState.PRESSED_WHEN_HIGH,
                    KeyEvent.KEYCODE_1);
            mButton1.register();
        } catch (IOException e) {
            Log.e(TAG, "failed to register button", e);
        }
        try {
            mButton2 = new ButtonInputDriver(
                    BUTTON2_PIN,
                    Button.LogicState.PRESSED_WHEN_HIGH,
                    KeyEvent.KEYCODE_2);
            mButton2.register();
        } catch (IOException e) {
            Log.e(TAG, "failed to register button", e);
        }
        try {
            mButton3 = new ButtonInputDriver(
                    BUTTON3_PIN,
                    Button.LogicState.PRESSED_WHEN_HIGH,
                    KeyEvent.KEYCODE_3);
            mButton3.register();
        } catch (IOException e) {
            Log.e(TAG, "failed to register button", e);
        }
        try {
            mButton4 = new ButtonInputDriver(
                    BUTTON4_PIN,
                    Button.LogicState.PRESSED_WHEN_HIGH,
                    KeyEvent.KEYCODE_4);
            mButton4.register();
        } catch (IOException e) {
            Log.e(TAG, "failed to register button", e);
        }
        try {
            mButton5 = new ButtonInputDriver(
                    BUTTON5_PIN,
                    Button.LogicState.PRESSED_WHEN_HIGH,
                    KeyEvent.KEYCODE_5);
            mButton5.register();
        } catch (IOException e) {
            Log.e(TAG, "failed to register button", e);
        }

        // Configure LEDs
        try {
            mLed1 = mPioService.openGpio(LED1_PIN);
            mLed1.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e) {
            Log.e(TAG, "failed to register LED", e);
        }
        try {
            mLed2 = mPioService.openGpio(LED2_PIN);
            mLed2.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e) {
            Log.e(TAG, "failed to register LED", e);
        }
        try {
            mLed3 = mPioService.openGpio(LED3_PIN);
            mLed3.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e) {
            Log.e(TAG, "failed to register LED", e);
        }
        try {
            mLed4 = mPioService.openGpio(LED4_PIN);
            mLed4.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e) {
            Log.e(TAG, "failed to register LED", e);
        }
        try {
            mLed5 = mPioService.openGpio(LED5_PIN);
            mLed5.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e) {
            Log.e(TAG, "failed to register LED", e);
        }

        mHandler.post(new Runnable() {
            private int mCounter;

            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                switch (mCounter) {
                    case 0:
                        setLed(1, true);
                        break;
                    case 1:
                        setLed(1, false);
                        setLed(2, true);
                        break;
                    case 2:
                        setLed(2, false);
                        setLed(3, true);
                        break;
                    case 3:
                        setLed(3, false);
                        setLed(4, true);
                        break;
                    case 4:
                        setLed(4, false);
                        setLed(5, true);
                        break;
                    case 5:
                        setLed(5, false);
                        mCounter = -1;
                        break;
                }
                mCounter++;
                mHandler.postDelayed(this, 500);
            }
        });

        String bridgeJson = Prefs.getString("last_bridge", null);
        if (bridgeJson != null) {
            HueBridge.Descriptor bridge = GsonUtils.get().fromJson(bridgeJson, HueBridge.Descriptor.class);
            debug("reconnecting to previous bridge " + bridge);
            mHueBridgeConnector.connect(bridge.getHost(), bridge.getBridgeId(),
                                        new CapabilitiesCallback() {
                                            @Override
                                            public void onSuccess(HueBridge hueBridge, CapabilitiesResponse success) {
                                                // Awesome, we're connected already
                                            }

                                            @Override
                                            public void onFailure(HueBridge hueBridge,
                                                                  ErrorResponse.ResponseError error, Throwable e) {
                                                startBridgeScan();
                                            }
                                        });
        } else if (BRIDGE_SCAN_ENABLED) {
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

                                              debug("Philips Hue bridge discovered: " + device);
                                              mHueBridgeConnector.connectLoop(device.getHost(), bridgeId);
                                              return true;
                                          } else {
                                              debug("Device discovered: " + device);
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
                                          debug("no UPnP device matches; retrying");
                                          throw new RuntimeException();
                                      }
                                      debug("UPnP device matches: " + devices.size());
                                      return devices;
                                  }
                              })
                              .retry()
                              .subscribe();
    }

    @Override
    protected void onStop() {
        if (mButton1 != null) {
            mButton1.unregister();
            try {
                mButton1.close();
            } catch (IOException ignore) {
            }
        }
        if (mButton2 != null) {
            mButton2.unregister();
            try {
                mButton2.close();
            } catch (IOException ignore) {
            }
        }
        if (mButton3 != null) {
            mButton3.unregister();
            try {
                mButton3.close();
            } catch (IOException ignore) {
            }
        }
        if (mButton4 != null) {
            mButton4.unregister();
            try {
                mButton4.close();
            } catch (IOException ignore) {
            }
        }
        if (mButton5 != null) {
            mButton5.unregister();
            try {
                mButton5.close();
            } catch (IOException ignore) {
            }
        }
        if (mLed1 != null) {
            try {
                mLed1.close();
            } catch (IOException ignore) {
            }
        }
        if (mLed2 != null) {
            try {
                mLed2.close();
            } catch (IOException ignore) {
            }
        }
        if (mLed3 != null) {
            try {
                mLed3.close();
            } catch (IOException ignore) {
            }
        }
        if (mLed4 != null) {
            try {
                mLed4.close();
            } catch (IOException ignore) {
            }
        }
        if (mLed5 != null) {
            try {
                mLed5.close();
            } catch (IOException ignore) {
            }
        }

        mHueBridgeConnector.stop();
        mInputMonitor.stop();

        super.onStop();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        debug("onKeyDown: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
                toggleLed(1);
                return true;
            case KeyEvent.KEYCODE_2:
                toggleLed(2);
                return true;
            case KeyEvent.KEYCODE_3:
                toggleLed(3);
                return true;
            case KeyEvent.KEYCODE_4:
                toggleLed(4);
                return true;
            case KeyEvent.KEYCODE_5:
                toggleLed(5);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void toggleLed(int id) {
        boolean value;
        switch (id) {
            case 1:
                mLed1On = !mLed1On;
                value = mLed1On;
                break;
            case 2:
                mLed2On = !mLed2On;
                value = mLed2On;
                break;
            case 3:
                mLed3On = !mLed3On;
                value = mLed3On;
                break;
            case 4:
                mLed4On = !mLed4On;
                value = mLed4On;
                break;
            case 5:
                mLed5On = !mLed5On;
                value = mLed5On;
                break;
            default:
                Log.e(TAG, "toggleLed: unexpected id " + id);
                return;
        }
        setLed(id, value);
    }

    public void setLed(int id, boolean value) {
        Gpio led;
        switch (id) {
            case 1:
                led = mLed1;
                break;
            case 2:
                led = mLed2;
                break;
            case 3:
                led = mLed3;
                break;
            case 4:
                led = mLed4;
                break;
            case 5:
                led = mLed5;
                break;
            default:
                Log.e(TAG, "toggleLed: unexpected id " + id);
                return;
        }
        if (led == null) {
            Log.e(TAG, "failed setting LED #" + id + "; LED not initialized");
            return;
        }
        try {
            debug("setting LED #" + "setting LED #" + id + " to " + value + " to " + "setting LED #" + id + " to " + value);
            led.setValue(value);
        } catch (IOException e) {
            Log.e(TAG, "failed setting LED #" + id, e);
        }
    }

    public void debug(String msg) {
        Log.d(TAG, msg);
        mTvInfo.setText(msg);
    }

}
