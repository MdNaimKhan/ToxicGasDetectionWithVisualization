package com.fahimshahrierrasel.gasmonitor.api;

import com.fahimshahrierrasel.gasmonitor.api.service.OutputService;
import com.fahimshahrierrasel.gasmonitor.api.service.TokenService;

public class ApiUtils {
    private static final String BASE_URL = "https://api.thinger.io";

    private ApiUtils() {
    }

    public static TokenService getTokenService() {
        return RetrofitClient.getClient(BASE_URL).create(TokenService.class);
    }

    public static OutputService getOutputService() {
        return RetrofitClient.getClient(BASE_URL).create(OutputService.class);
    }
}
