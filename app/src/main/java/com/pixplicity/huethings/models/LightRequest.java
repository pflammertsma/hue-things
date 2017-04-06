package com.pixplicity.huethings.models;

import com.google.gson.annotations.SerializedName;

public class LightRequest {

    @SerializedName("on")
    boolean switchedOn;

    @SerializedName("hue")
    int hue;

    @SerializedName("sat")
    int saturation;

    @SerializedName("bri")
    int brightness;

    public LightRequest(boolean switchedOn, int hue, int saturation, int brightness) {
        this.switchedOn = switchedOn;
        this.hue = hue;
        this.saturation = saturation;
        this.brightness = brightness;
    }

}
