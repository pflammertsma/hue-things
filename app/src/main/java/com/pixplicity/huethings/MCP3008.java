package com.pixplicity.huethings;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

public class MCP3008 {

    private final String csPin;
    private final String clockPin;
    private final String mosiPin;
    private final String misoPin;

    private Gpio mCsPin;
    private Gpio mClockPin;
    private Gpio mMosiPin;
    private Gpio mMisoPin;

    public MCP3008(String csPin, String clockPin, String mosiPin, String misoPin) {
        this.csPin = csPin;
        this.clockPin = clockPin;
        this.mosiPin = mosiPin;
        this.misoPin = misoPin;
    }

    public void register() throws IOException {
        PeripheralManagerService service = new PeripheralManagerService();
        mClockPin = service.openGpio(clockPin);
        mCsPin = service.openGpio(csPin);
        mMosiPin = service.openGpio(mosiPin);
        mMisoPin = service.openGpio(misoPin);

        mClockPin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        mCsPin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        mMosiPin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        mMisoPin.setDirection(Gpio.DIRECTION_IN);
    }

    public int readAdc(int channel) throws IOException {
        if (channel < 0 || channel > 7) {
            throw new IOException("ADC channel must be between 0 and 7");
        }

        initReadState();
        initChannelSelect(channel);

        return getValueFromSelectedChannel();
    }

    private int getValueFromSelectedChannel() throws IOException {
        int value = 0x0;

        for (int i = 0; i < 12; i++) {
            toggleClock();

            value <<= 0x1;
            if (mMisoPin.getValue()) {
                value |= 0x1;
            }
        }

        mCsPin.setValue(true);

        value >>= 0x1; // first bit is 'null', so drop it

        return value;
    }

    private void initReadState() throws IOException {
        mCsPin.setValue(true);
        mClockPin.setValue(false);
        mCsPin.setValue(false);
    }

    private void initChannelSelect(int channel) throws IOException {
        int commandout = channel;
        commandout |= 0x18; // start bit + single-ended bit
        commandout <<= 0x3; // we only need to send 5 bits

        for (int i = 0; i < 5; i++) {

            if ((commandout & 0x80) != 0x0) {
                mMosiPin.setValue(true);
            } else {
                mMosiPin.setValue(false);
            }

            commandout <<= 0x1;

            toggleClock();
        }
    }

    private void toggleClock() throws IOException {
        mClockPin.setValue(true);
        mClockPin.setValue(false);
    }

    public void unregister() {
        if (mCsPin != null) {
            try {
                mCsPin.close();
            } catch (IOException ignore) {
                // do nothing
            }
        }

        if (mClockPin != null) {
            try {
                mClockPin.close();
            } catch (IOException ignore) {
                // do nothing
            }
        }

        if (mMisoPin != null) {
            try {
                mMisoPin.close();
            } catch (IOException ignore) {
                // do nothing
            }
        }

        if (mMosiPin != null) {
            try {
                mMosiPin.close();
            } catch (IOException ignore) {
                // do nothing
            }
        }

    }

}