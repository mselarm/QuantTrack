package com.upv.quanttrack.data;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
public interface AlphaVantageApi {
    // Definimos el endpoint y los parámetros de la URL
    // Ejemplo de URL final: /query?function=TIME_SERIES_DAILY&symbol=AAPL&apikey=TU_CLAVE
    @GET("query?function=TIME_SERIES_DAILY")
    Call<AlphaVantageResponse> getDailyData(
            @Query("symbol") String ticker,
            @Query("apikey") String apiKey
    );
}
