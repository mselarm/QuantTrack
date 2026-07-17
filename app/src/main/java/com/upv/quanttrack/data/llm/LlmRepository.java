package com.upv.quanttrack.data.llm;

import android.util.Log;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LlmRepository {

    // INYECTA TU CLAVE AQUÍ
    private static final String API_KEY = "Tu clave de Gemini";
    private LlmApi api;

    // Puente de comunicación asíncrona para la interfaz
    public interface LlmCallback {
        void onSuccess(String analysis);
        void onError(String error);
    }

    public LlmRepository() {
        api = LlmClient.getClient().create(LlmApi.class);
    }

    public void analyzeMarket(String prompt, LlmCallback callback) {
        LlmRequest request = new LlmRequest(prompt);
        Call<LlmResponse> call = api.analyzeData(API_KEY, request);

        call.enqueue(new Callback<LlmResponse>() {
            @Override
            public void onResponse(Call<LlmResponse> call, Response<LlmResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getAnswer());
                } else {
                    callback.onError("Error del LLM HTTP: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<LlmResponse> call, Throwable t) {
                callback.onError("Fallo de red hacia Gemini: " + t.getMessage());
            }
        });
    }
}
