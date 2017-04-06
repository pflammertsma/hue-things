package com.pixplicity.huethings.network;

import com.pixplicity.huethings.models.AuthResponse;
import com.pixplicity.huethings.models.ErrorResponse;

import java.io.IOException;

public interface AuthenticationCallback {

    void onSuccess(AuthResponse.AuthResponseSuccess success);

    void onFailure(ErrorResponse.ResponseError error, IOException e);

}
