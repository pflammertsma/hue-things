package com.pixplicity.huethings.models;

import com.google.gson.annotations.SerializedName;

public class ErrorResponse {

    @SerializedName("error")
    public ResponseError error;

    public class ResponseError {

        @SerializedName("type")
        public int type;


        @SerializedName("address")
        public String address;


        @SerializedName("description")
        public String description;

    }

}
