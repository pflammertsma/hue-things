package com.pixplicity.huethings;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.PeripheralManagerService;
import com.pixplicity.huethings.gpio.InputMonitor;
import com.pixplicity.huethings.network.HueBridgeConnector;
import com.pixplicity.huethings.upnp.UPnPDevice;
import com.pixplicity.huethings.upnp.UPnPDeviceFinder;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
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

        new UPnPDeviceFinder().observe()
                              .filter(new Func1<UPnPDevice, Boolean>() {
                                  @Override
                                  public Boolean call(UPnPDevice device) {
                                      try {
                                          device.downloadSpecs();
                                      } catch (Exception e) {
                                          // Ignore errors
                                          Log.e(TAG, "failed obtaining device specs", e);
                                          return false;
                                      }
                                      return true;
                                  }
                              })
                              .subscribeOn(Schedulers.io())
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribe(new Action1<UPnPDevice>() {
                                  @Override
                                  public void call(UPnPDevice device) {
                                      String bridgeId = device.getProperty("upnp_hue-bridgeid");
                                      if (bridgeId != null) {
                                          Log.d(TAG, "Philips Hue bridge discovered: " + device);
                                          mHueBridgeConnector.connect(device.getHost(), bridgeId);
                                      } else {
                                          Log.d(TAG, "Device discovered: " + device);
                                      }
                                  }
                              });
    }

    @Override
    protected void onStop() {
        mHueBridgeConnector.stop();
        mInputMonitor.stop();

        super.onStop();
    }

}