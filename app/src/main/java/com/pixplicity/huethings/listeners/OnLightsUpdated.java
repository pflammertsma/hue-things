package com.pixplicity.huethings.listeners;

public interface OnLightsUpdated {

    void onLightUpdated(String lightId);

    void onLightUpdateFailed(String lightId, Throwable e);

    void onLightsUpdated(int count);

}
