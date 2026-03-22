package com.upv.quanttrack.domain.math;

import com.upv.quanttrack.data.DailyData;
import java.util.ArrayList;
import java.util.List;

public class OscillatorEngine {

    /**
     * Calcula el RSI (Relative Strength Index) con el suavizado de Wilder.
     * @param data Lista cronológica de datos diarios.
     * @param period Periodo estándar (usualmente 14).
     * @return Lista de valores RSI alineados con la lista original.
     * Los primeros 'period' elementos serán null o 0 por falta de datos.
     */
    public static List<Double> calculateRSI(List<DailyData> data, int period) {
        List<Double> rsiList = new ArrayList<>();
        if (data.size() < period + 1) {
            for (int i = 0; i < data.size(); i++) rsiList.add(0.0);
            return rsiList;
        }

        double sumGain = 0;
        double sumLoss = 0;

        // 1. Añadimos 0 para el primer dato (no hay retorno previo)
        rsiList.add(0.0);

        // 2. Cálculo de la primera media simple (SMA) para la ventana inicial
        for (int i = 1; i <= period; i++) {
            double change = data.get(i).getClose() - data.get(i - 1).getClose();
            if (change > 0) sumGain += change;
            else sumLoss += Math.abs(change);
            rsiList.add(0.0); // Rellenamos con 0 hasta tener la muestra inicial
        }

        double avgGain = sumGain / period;
        double avgLoss = sumLoss / period;

        // 3. Suavizado de Wilder (Exponential Moving Average modificado) para el resto del tensor
        for (int i = period + 1; i < data.size(); i++) {
            double change = data.get(i).getClose() - data.get(i - 1).getClose();
            double gain = Math.max(0, change);
            double loss = Math.max(0, -change);

            avgGain = ((avgGain * (period - 1)) + gain) / period;
            avgLoss = ((avgLoss * (period - 1)) + loss) / period;

            if (avgLoss == 0) {
                rsiList.add(100.0);
            } else {
                double rs = avgGain / avgLoss;
                double rsi = 100.0 - (100.0 / (1 + rs));
                rsiList.add(rsi);
            }
        }

        return rsiList;
    }
}
