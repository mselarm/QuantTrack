package com.upv.quanttrack.domain.math;
public class SimpleMovingAverage {

    private final int period;

    public SimpleMovingAverage(int period) {
        if (period <= 0) {
            throw new IllegalArgumentException("El periodo debe ser mayor que cero.");
        }
        this.period = period;
    }

    public double[] calculate(double[] prices) {
        if (prices == null || prices.length < period) {
            return new double[0];
        }

        double[] sma = new double[prices.length];
        double windowSum = 0;

        // 1. Calcular la suma de la primera ventana
        for (int i = 0; i < period; i++) {
            windowSum += prices[i];
            sma[i] = Double.NaN; // No hay suficiente histórico aún
        }

        // El primer valor de la media
        sma[period - 1] = windowSum / period;

        // 2. Ventana deslizante: O(N)
        // Sumamos el nuevo que entra, restamos el que sale por la izquierda
        for (int i = period; i < prices.length; i++) {
            windowSum += prices[i] - prices[i - period];
            sma[i] = windowSum / period;
        }

        return sma;
    }
}