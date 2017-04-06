package com.pixplicity.huethings.network;

import com.pixplicity.huethings.models.LightsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface HueBridgeInterface {

    @GET("lights")
    Call<LightsResponse> lightsList();

    @GET("lights/{lightId}/status")
    Call<Void> lightStatus(@Path("lightId") String lightId);

}
