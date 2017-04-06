package com.pixplicity.huethings.models;

import com.google.gson.annotations.SerializedName;

public class AuthResponse extends ErrorResponse {

    @SerializedName("success")
    public AuthResponseSuccess success;

    public static class AuthResponseSuccess {

        @SerializedName("username")
        public String username;

    }

}
