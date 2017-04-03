package com.pixplicity.huethings;

import com.google.gson.Gson;

public class GsonUtils {

    private static Gson sGson;

    public static Gson get() {
        if (sGson == null) {
            sGson = new Gson();
        }
        return sGson;
    }

}
