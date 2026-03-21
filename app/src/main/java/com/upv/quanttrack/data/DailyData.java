package com.upv.quanttrack.data;

public class DailyData {

    private final double open;
    private final double high;
    private final double low;
    private final double close;

    // Constructor que usa el MarketRepository al leer el CSV
    public DailyData(double open, double high, double low, double close) {
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
    }

    public double getOpen() { return open; }
    public double getHigh() { return high; }
    public double getLow() { return low; }
    public double getClose() { return close; }
}

