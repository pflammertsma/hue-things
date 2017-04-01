package com.pixplicity.huethings.network;

import java.io.IOException;

public interface CapabilitiesCallback {

    void onSuccess(HueBridge.CapabilitiesResponse success);

    void onFailure(HueBridge.HueResponse.ResponseError error, IOException e);

}
