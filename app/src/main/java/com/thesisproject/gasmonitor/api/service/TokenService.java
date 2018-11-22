package com.thesisproject.gasmonitor.api.service;

import com.thesisproject.gasmonitor.model.Token;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface TokenService {
    @FormUrlEncoded
    @POST("/oauth/token")
    Call<Token> getToken(@Field("grant_type") String grantType, @Field("username") String username,
                         @Field("password") String password);
}
