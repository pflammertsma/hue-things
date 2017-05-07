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

    private static final String BUTTON1_PIN = "BCM16";
    private static final String BUTTON2_PIN = "BCM19";
    private static final String BUTTON3_PIN = "BCM13";
    private static final String BUTTON4_PIN = "BCM6";
    private static final String BUTTON5_PIN = "BCM5";
    private static final String LED1_PIN = "BCM12";
    private static final String LED2_PIN = "BCM25";
    private static final String LED3_PIN = "BCM24";
    private static final String LED4_PIN = "BCM23";
    private static final String LED5_PIN = "BCM18";

    public static final int ANIMATION_SPEED_MS = 160;
    public static final int ANIMATION_PAUSE_MS = 480;

    private PeripheralManagerService mPioService;
    private InputMonitor mInputMonitor = new InputMonitor();
    private HueBridgeConnector mHueBridgeConnector = new HueBridgeConnector(mInputMonitor);
    private ButtonInputDriver mButton1, mButton2, mButton3, mButton4, mButton5;
    private Gpio mLed1, mLed2, mLed3, mLed4, mLed5;
    private boolean mLed1On, mLed2On, mLed3On, mLed4On, mLed5On;

    private final Handler mHandler = new Handler();

    private TextView mTvInfo1, mTvInfo2, mTvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTvInfo1 = (TextView) findViewById(R.id.tv_info1);
        mTvInfo2 = (TextView) findViewById(R.id.tv_info2);
        mTvError = (TextView) findViewById(R.id.tv_error);

        mPioService = new PeripheralManagerService();
        debug("gpios: " + mPioService.getGpioList(), mTvInfo1);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Configure buttons
        try {
            mButton1 = new ButtonInputDriver(
                    BUTTON1_PIN,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_1);
            mButton1.register();
        } catch (IOException e) {
            Log.e(TAG, "failed to register button", e);
        }
        try {
            mButton2 = new ButtonInputDriver(
                    BUTTON2_PIN,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_2);
            mButton2.register();
        } catch (IOException e) {
            Log.e(TAG, "failed to register button", e);
        }
        try {
            mButton3 = new ButtonInputDriver(
                    BUTTON3_PIN,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_3);
            mButton3.register();
        } catch (IOException e) {
            Log.e(TAG, "failed to register button", e);
        }
        try {
            mButton4 = new ButtonInputDriver(
                    BUTTON4_PIN,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_4);
            mButton4.register();
        } catch (IOException e) {
            Log.e(TAG, "failed to register button", e);
        }
        try {
            mButton5 = new ButtonInputDriver(
                    BUTTON5_PIN,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_5);
            mButton5.register();
        } catch (IOException e) {
            Log.e(TAG, "failed to register button", e);
        }

        openLeds();

        mHandler.post(new Runnable() {
            private int mCounter;

            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                int delay;
                if (true) {
                    delay = animation2(mCounter);
                } else {
                    delay = animation1(mCounter);
                }
                if (delay < 0) {
                    mCounter = 0;
                    run();
                    return;
                }
                mCounter++;
                mHandler.postDelayed(this, delay);
            }

            private int animation1(int counter) {
                switch (counter) {
                    case 0:
                        setLed(1, true);
                        return ANIMATION_SPEED_MS;
                    case 1:
                        setLed(1, false);
                        setLed(2, true);
                        return ANIMATION_SPEED_MS;
                    case 2:
                        setLed(2, false);
                        setLed(3, true);
                        return ANIMATION_SPEED_MS;
                    case 3:
                        setLed(3, false);
                        setLed(4, true);
                        return ANIMATION_SPEED_MS;
                    case 4:
                        setLed(4, false);
                        setLed(5, true);
                        return ANIMATION_SPEED_MS;
                    case 5:
                        setLed(5, false);
                        return ANIMATION_PAUSE_MS;
                    default:
                        return -1;
                }
            }

            private int animation2(int counter) {
                switch (counter) {
                    case 0:
                        setLed(1, true);
                        setLed(2, true);
                        return ANIMATION_SPEED_MS;
                    case 1:
                        setLed(1, false);
                        setLed(2, false);
                        return ANIMATION_SPEED_MS;
                    default:
                        return -1;
                }
            }

        });

        String bridgeJson = Prefs.getString("last_bridge", null);
        if (bridgeJson != null) {
            HueBridge.Descriptor bridge = GsonUtils.get().fromJson(bridgeJson, HueBridge.Descriptor.class);
            debug("reconnecting to previous bridge " + bridge, mTvInfo1);
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
        closeLeds();

        mHueBridgeConnector.stop();
        mInputMonitor.stop();

        super.onStop();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        debug("onKeyDown: " + keyCode, mTvInfo2);
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

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        debug("onKeyUp: " + keyCode, mTvInfo2);
        return super.onKeyUp(keyCode, event);
    }

    private void openLeds() {
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
    }

    private void closeLeds() {
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

                                              debug("Philips Hue bridge discovered: " + device, mTvInfo2);
                                              mHueBridgeConnector.connectLoop(device.getHost(), bridgeId);
                                              return true;
                                          } else {
                                              debug("Device discovered: " + device, mTvInfo1);
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
                                          debug("no UPnP device matches; retrying", mTvError);
                                          throw new RuntimeException();
                                      }
                                      debug("UPnP device matches: " + devices.size(), mTvInfo1);
                                      return devices;
                                  }
                              })
                              .retry()
                              .subscribe();
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
        debug("set LED #" + id + " to " + value, mTvInfo1);
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
            led.setValue(value);
        } catch (IOException e) {
            Log.e(TAG, "failed setting LED #" + id, e);
        }
    }

    public void debug(String msg, TextView textView) {
        Log.d(TAG, msg);
        textView.setText(msg);
    }

}
