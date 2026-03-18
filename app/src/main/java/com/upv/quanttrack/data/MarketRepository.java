package com.upv.quanttrack.data;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;
import java.util.Map;

public class MarketRepository {

    // Constante de la API Key
    private static final String API_KEY = "F8GRLBWMGTCIAJTA";

    private AlphaVantageApi api;

    // 1. Definimos el contrato de retorno (El puente hacia la Interfaz Gráfica)
    public interface DataCallback {
        void onSuccess(Map<String, DailyData> data);
        void onError(String error);
    }

    public MarketRepository() {
        // Inicializamos la interfaz usando el cliente Singleton
        api = ApiClient.getClient().create(AlphaVantageApi.class);
    }

    // 2. Modificamos la firma: ahora recibe el ticker y el canal de retorno (callback)
    public void fetchDailyData(String ticker, DataCallback callback) {

        Call<AlphaVantageResponse> call = api.getDailyData(ticker, API_KEY);

        call.enqueue(new Callback<AlphaVantageResponse>() {
            @Override
            public void onResponse(Call<AlphaVantageResponse> call, Response<AlphaVantageResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getTimeSeries() != null) {

                    // Éxito topológico: Devolvemos el mapa de datos completo al hilo principal (MainActivity)
                    callback.onSuccess(response.body().getTimeSeries());

                } else {
                    // Si el servidor de AlphaVantage devuelve un límite de cuota o error
                    callback.onError("Error HTTP del servidor: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<AlphaVantageResponse> call, Throwable t) {
                // Fallo físico (ej. modo avión activado)
                callback.onError("Fallo físico de red: " + t.getMessage());
            }
        });
    }
}
