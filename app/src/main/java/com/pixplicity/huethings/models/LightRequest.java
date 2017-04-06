package com.pixplicity.huethings.models;

import com.google.gson.annotations.SerializedName;

public class LightRequest {

    @SerializedName("on")
    private boolean switchedOn;

    @SerializedName("hue")
    private Integer hue;

    @SerializedName("sat")
    private Integer saturation;

    @SerializedName("bri")
    private Integer brightness;

    public LightRequest(boolean switchedOn, Integer hue, Integer saturation, Integer brightness) {
        this.switchedOn = switchedOn;
        this.hue = hue;
        this.saturation = saturation;
        this.brightness = brightness;
    }

}
