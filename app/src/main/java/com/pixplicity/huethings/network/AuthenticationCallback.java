package com.pixplicity.huethings.network;

import java.io.IOException;

public interface AuthenticationCallback {

    void onSuccess(HueBridge.AuthResponse.AuthResponseSuccess success);

    void onFailure(HueBridge.HueResponse.ResponseError error, IOException e);

}
