package com.thesisproject.gasmonitor.api.service;

import com.thesisproject.gasmonitor.model.Output;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface OutputService {
    @GET("/v2/users/naimKhan/devices/{device_id}/AIRQ")
    Call<Output> getDeviceOutputAirQuality(@Header ("Authorization") String token, @Header("Accept") String accept, @Path("device_id") String deviceId);
    @GET("/v2/users/naimKhan/devices/{device_id}/ALCOHOL")
    Call<Output> getDeviceOutputAlcohol(@Header ("Authorization") String token, @Header("Accept") String accept, @Path("device_id") String deviceId);
    @GET("/v2/users/naimKhan/devices/{device_id}/CO")
    Call<Output> getDeviceOutputCO(@Header ("Authorization") String token, @Header("Accept") String accept, @Path("device_id") String deviceId);


}
