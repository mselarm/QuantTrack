package com.upv.quanttrack.data.macro;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

public interface TreasuryApi {
    // URL directa al archivo CSV del Tesoro. 100% libre y público.
    @GET("resource-center/data-chart-center/interest-rates/daily-treasury-rates.csv/2026/all?type=daily_treasury_yield_curve&field_tdr_date_value=2026&page&_format=csv")
    Call<ResponseBody> getYieldCurveCsv();
}