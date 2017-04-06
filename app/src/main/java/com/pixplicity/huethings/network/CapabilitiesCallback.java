package com.pixplicity.huethings.network;

import com.pixplicity.huethings.models.CapabilitiesResponse;
import com.pixplicity.huethings.models.ErrorResponse;

import java.io.IOException;

public interface CapabilitiesCallback {

    void onSuccess(HueBridge hueBridge, CapabilitiesResponse success);

    void onFailure(HueBridge hueBridge, ErrorResponse.ResponseError error, IOException e);

}
