package com.upv.quanttrack.data.macro;

import com.google.gson.annotations.SerializedName;

public class YieldCurveData {
    @SerializedName("record_date")
    public String date;

    @SerializedName("tc_1month") public String m1;
    @SerializedName("tc_3month") public String m3;
    @SerializedName("tc_6month") public String m6;
    @SerializedName("tc_1year")  public String y1;
    @SerializedName("tc_2year")  public String y2;
    @SerializedName("tc_3year")  public String y3;
    @SerializedName("tc_5year")  public String y5;
    @SerializedName("tc_7year")  public String y7;
    @SerializedName("tc_10year") public String y10;
    @SerializedName("tc_20year") public String y20;
    @SerializedName("tc_30year") public String y30;

    // Método seguro de parseo (a veces el gobierno devuelve null si ese día no hubo subasta)
    public double getRate(String rateStr) {
        try {
            return (rateStr != null && !rateStr.isEmpty()) ? Double.parseDouble(rateStr) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
