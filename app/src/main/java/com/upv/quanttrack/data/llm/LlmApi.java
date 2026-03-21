package com.upv.quanttrack.data.llm;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface LlmApi {
    // Usamos el modelo flash por velocidad
    @POST("v1beta/models/gemini-3.1-flash-lite-preview:generateContent")
    Call<LlmResponse> analyzeData(
            @Query("key") String apiKey,
            @Body LlmRequest request
    );
}