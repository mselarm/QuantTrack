package com.upv.quanttrack.data;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface YahooFinanceApi {
    // Usamos el endpoint v8 de gráficos, que no exige validación por 'crumb'
    @GET("v8/finance/chart/{ticker}")
    Call<String> getHistoricalData(
            @Path("ticker") String ticker,
            @Query("interval") String interval,
            @Query("range") String range
    );
}