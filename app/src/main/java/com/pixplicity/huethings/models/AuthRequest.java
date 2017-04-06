package com.pixplicity.huethings.models;

import com.google.gson.annotations.SerializedName;

public class AuthRequest {

    @SerializedName("devicetype")
    String deviceType;

    public AuthRequest(String deviceType) {
        this.deviceType = deviceType;
    }

}
