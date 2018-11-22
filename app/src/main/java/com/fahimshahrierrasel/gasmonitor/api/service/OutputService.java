package com.fahimshahrierrasel.gasmonitor.api.service;

import com.fahimshahrierrasel.gasmonitor.model.Output;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface OutputService {
    @GET("/v2/users/naimKhan/devices/{device_id}/ouput")
    Call<Output> getDeviceOutput(@Header ("Authorization") String token, @Header("Accept") String accept, @Path("device_id") String deviceId);

}
