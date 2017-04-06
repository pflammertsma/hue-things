package com.pixplicity.huethings.models;

import com.google.gson.annotations.SerializedName;

public class CapabilitiesResponse {

    @SerializedName("lights")
    Availability lights;

    public class Availability {

        @SerializedName("available")
        int available;

    }

}
