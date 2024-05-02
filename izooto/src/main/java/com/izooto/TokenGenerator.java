package com.izooto;


import android.content.Context;

import android.content.Context;

public interface TokenGenerator {

    void getToken(Context context, String senderId,TokenGenerationHandler callback);

    interface TokenGenerationHandler {
        void complete(String id);

        void failure(String errorMsg);
    }
}
