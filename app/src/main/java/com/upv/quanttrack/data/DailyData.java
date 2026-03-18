package com.upv.quanttrack.data;
import com.google.gson.annotations.SerializedName;

public class DailyData {

    @SerializedName("1. open")
    private String open;

    @SerializedName("2. high")
    private String high;

    @SerializedName("3. low")
    private String low;

    @SerializedName("4. close")
    private String close;

    @SerializedName("5. volume")
    private String volume;

    // Getters para extraer la información y convertirla a numérico
    public double getOpen() { return Double.parseDouble(open); }
    public double getHigh() { return Double.parseDouble(high); }
    public double getLow() { return Double.parseDouble(low); }
    public double getClose() { return Double.parseDouble(close); }
    public double getVolume() { return Double.parseDouble(volume); }
}

