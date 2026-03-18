package com.upv.quanttrack.data;
import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class AlphaVantageResponse {

    // Usamos un Map porque las fechas (claves) cambian cada día
    @SerializedName("Time Series (Daily)")
    private Map<String, DailyData> timeSeries;

    public Map<String, DailyData> getTimeSeries() {
        return timeSeries;
    }
}