package com.upv.quanttrack.domain.math;

import com.upv.quanttrack.data.DailyData;
import java.util.Arrays;
import java.util.List;

public class VolatilityEngine {

    private static final int TRADING_DAYS = 252;

    /**
     * Rango Verdadero Medio (ATR). Mide el riesgo de recorrido intradía en valor absoluto ($).
     * TR = max(H - L, |H - C_prev|, |L - C_prev|)
     */
    public static double calculateATR(List<DailyData> data, int period) {
        if (data.size() < period + 1) return 0.0;

        double[] tr = new double[data.size() - 1];
        for (int i = 1; i < data.size(); i++) {
            DailyData current = data.get(i);
            DailyData prev = data.get(i - 1);

            double hl = current.getHigh() - current.getLow();
            double hcp = Math.abs(current.getHigh() - prev.getClose());
            double lcp = Math.abs(current.getLow() - prev.getClose());

            tr[i - 1] = Math.max(hl, Math.max(hcp, lcp));
        }

        // Media móvil simple (SMA) de los True Ranges
        double sumTR = 0;
        for (int i = tr.length - period; i < tr.length; i++) {
            sumTR += tr[i];
        }
        return sumTR / period;
    }

    /**
     * Volumen Relativo (RVOL).
     * RVOL = Vol_actual / SMA(Vol_20)
     */
    public static double calculateRVOL(List<DailyData> data, int period) {
        if (data.size() < period) return 0.0;

        double sumVol = 0;
        for (int i = data.size() - period - 1; i < data.size() - 1; i++) {
            sumVol += data.get(i).getVolume();
        }
        double avgVol = sumVol / period;
        double currentVol = data.get(data.size() - 1).getVolume();

        return avgVol > 0 ? currentVol / avgVol : 0.0;
    }

    /**
     * Retornos logarítmicos: R_t = ln(P_t / P_{t-1})
     */
    public static double[] calculateLogReturns(double[] prices) {
        double[] returns = new double[prices.length - 1];
        for (int i = 1; i < prices.length; i++) {
            returns[i - 1] = Math.log(prices[i] / prices[i - 1]);
        }
        return returns;
    }

    /**
     * Varianza y Desviación Estándar Anualizada (σ).
     */
    public static double calculateAnnualizedSigma(double[] returns) {
        double mean = 0.0;
        for (double r : returns) mean += r;
        mean /= returns.length;

        double var = 0.0;
        for (double r : returns) {
            var += Math.pow(r - mean, 2);
        }
        var /= (returns.length - 1); // Varianza muestral

        return Math.sqrt(var) * Math.sqrt(TRADING_DAYS);
    }

    /**
     * Percentil de Volatilidad IVP (Implied Volatility Percentile equivalent for Historical).
     * Compara la ventana actual de 20 días con todas las ventanas de 20 días del último año.
     */
    public static double calculateVolatilityPercentile(double[] returns, int window) {
        if (returns.length < window * 2) return 0.0;

        int numWindows = returns.length - window + 1;
        double[] historicalSigmas = new double[numWindows];

        for (int i = 0; i < numWindows; i++) {
            double[] windowReturns = Arrays.copyOfRange(returns, i, i + window);
            historicalSigmas[i] = calculateAnnualizedSigma(windowReturns);
        }

        double currentSigma = historicalSigmas[numWindows - 1];

        // Contamos cuántas sigmas históricas son menores que la actual
        int countLower = 0;
        for (double sig : historicalSigmas) {
            if (sig < currentSigma) countLower++;
        }

        return (double) countLower / numWindows;
    }

    /**
     * Beta (β) de un activo respecto a un Benchmark.
     * β = Cov(Ra, Rm) / Var(Rm)
     */
    public static double calculateBeta(double[] assetReturns, double[] marketReturns) {
        int n = Math.min(assetReturns.length, marketReturns.length);

        double meanA = 0, meanM = 0;
        for(int i = 0; i < n; i++) {
            meanA += assetReturns[i];
            meanM += marketReturns[i];
        }
        meanA /= n;
        meanM /= n;

        double cov = 0, varM = 0;
        for(int i = 0; i < n; i++) {
            double diffA = assetReturns[i] - meanA;
            double diffM = marketReturns[i] - meanM;
            cov += diffA * diffM;
            varM += diffM * diffM;
        }

        return varM == 0 ? 0 : cov / varM;
    }
}
