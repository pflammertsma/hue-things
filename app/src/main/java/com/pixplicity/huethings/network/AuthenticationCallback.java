package com.pixplicity.huethings.network;

import com.pixplicity.huethings.models.AuthResponse;
import com.pixplicity.huethings.models.ErrorResponse;

public interface AuthenticationCallback {

    void onSuccess(AuthResponse.AuthResponseSuccess success);

    void onFailure(ErrorResponse.ResponseError error, Throwable e);

}
