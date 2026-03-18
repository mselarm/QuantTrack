package com.upv.quanttrack.data.llm;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface LlmApi {
    // Usamos el modelo flash por velocidad
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    Call<LlmResponse> analyzeData(
            @Query("key") String apiKey,
            @Body LlmRequest request
    );
}