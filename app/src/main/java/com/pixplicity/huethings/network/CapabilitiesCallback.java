package com.pixplicity.huethings.network;

import java.io.IOException;

public interface CapabilitiesCallback {

    void onSuccess(HueBridge hueBridge, HueBridge.CapabilitiesResponse success);

    void onFailure(HueBridge hueBridge, HueBridge.ErrorResponse.ResponseError error, IOException e);

}
