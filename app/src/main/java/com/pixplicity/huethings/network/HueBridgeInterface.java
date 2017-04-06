package com.pixplicity.huethings.network;

import com.pixplicity.huethings.models.AuthRequest;
import com.pixplicity.huethings.models.AuthResponse;
import com.pixplicity.huethings.models.CapabilitiesResponse;
import com.pixplicity.huethings.models.LightRequest;
import com.pixplicity.huethings.models.LightsResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface HueBridgeInterface {

    @POST()
    Call<AuthResponse[]> authenticate(@Body AuthRequest body);

    @GET("capabilities")
    Call<CapabilitiesResponse> capabilities();

    @GET("lights")
    Call<LightsResponse> lightsList();

    @PUT("lights/{lightId}")
    Call<Void> lightUpdate(@Path("lightId") String lightId, @Body LightRequest body);

    @GET("lights/{lightId}/status")
    Call<Void> lightStatus(@Path("lightId") LightRequest lightId);

}
