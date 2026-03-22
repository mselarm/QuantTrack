package com.upv.quanttrack.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.upv.quanttrack.R;
import com.upv.quanttrack.data.DailyData;
import com.upv.quanttrack.data.MarketRepository;
import com.upv.quanttrack.domain.math.VolatilityEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class VolatilityFragment extends Fragment implements Analyzable {

    private EditText etVolTicker;
    private Button btnAnalyzeVol;
    private TextView tvBeta, tvVolPercentile, tvATR, tvRVOL;
    private CombinedChart volCombinedChart;

    private final MarketRepository repository = new MarketRepository();

    // Guardamos las métricas para inyectarlas al prompt de la IA
    private double currentBeta = 0;
    private double currentPercentile = 0;
    private double currentATR = 0;
    private double currentRVOL = 0;
    private String currentTicker = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_volatility, container, false);

        etVolTicker = view.findViewById(R.id.etVolTicker);
        btnAnalyzeVol = view.findViewById(R.id.btnAnalyzeVol);
        tvBeta = view.findViewById(R.id.tvBeta);
        tvVolPercentile = view.findViewById(R.id.tvVolPercentile);
        tvATR = view.findViewById(R.id.tvATR);
        tvRVOL = view.findViewById(R.id.tvRVOL);
        volCombinedChart = view.findViewById(R.id.volCombinedChart);

        configurarLienzo();

        btnAnalyzeVol.setOnClickListener(v -> analizarActivo());

        return view;
    }

    private void analizarActivo() {
        String ticker = etVolTicker.getText().toString().trim().toUpperCase();
        if (ticker.isEmpty()) return;

        btnAnalyzeVol.setEnabled(false);
        btnAnalyzeVol.setText("DESCARGANDO TENSORES...");

        // Usamos un mapa concurrente para guardar ambas respuestas
        Map<String, Map<String, DailyData>> multiData = new ConcurrentHashMap<>();
        AtomicInteger callbacksCompleted = new AtomicInteger(0);
        String benchmark = "^GSPC"; // S&P 500

        // 1. Petición del Activo
        repository.fetchDailyData(ticker, new MarketRepository.DataCallback() {
            @Override
            public void onSuccess(Map<String, DailyData> data) {
                multiData.put(ticker, data);
                verificarBarrera(callbacksCompleted.incrementAndGet(), ticker, benchmark, multiData);
            }
            @Override
            public void onError(String error) {
                manejarErrorRed(error);
            }
        });

        // 2. Petición del Benchmark (S&P 500) en paralelo
        repository.fetchDailyData(benchmark, new MarketRepository.DataCallback() {
            @Override
            public void onSuccess(Map<String, DailyData> data) {
                multiData.put(benchmark, data);
                verificarBarrera(callbacksCompleted.incrementAndGet(), ticker, benchmark, multiData);
            }
            @Override
            public void onError(String error) {
                manejarErrorRed("Error S&P 500: " + error);
            }
        });
    }

    private void manejarErrorRed(String error) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            btnAnalyzeVol.setEnabled(true);
            btnAnalyzeVol.setText("ANALIZAR");
            Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
        });
    }

    private void verificarBarrera(int completados, String ticker, String benchmark, Map<String, Map<String, DailyData>> multiData) {
        if (completados == 2) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> procesarAlgebra(ticker, multiData.get(ticker), multiData.get(benchmark)));
            }
        }
    }

    private void procesarAlgebra(String ticker, Map<String, DailyData> assetData, Map<String, DailyData> marketData) {
        // 1. Alineación Topológica (Intersección de fechas)
        Set<String> commonDates = new HashSet<>(assetData.keySet());
        commonDates.retainAll(marketData.keySet());

        if (commonDates.size() < 30) {
            manejarErrorRed("Datos insuficientes para análisis estadístico (mínimo 30 días).");
            return;
        }

        List<String> sortedDates = new ArrayList<>(commonDates);
        Collections.sort(sortedDates);

        int n = sortedDates.size();
        List<DailyData> assetDailyData = new ArrayList<>();
        double[] assetPrices = new double[n];
        double[] marketPrices = new double[n];

        // DECLARACIÓN FALTANTE: Lista para guardar el TR y pasarlo al gráfico
        List<Double> trueRangePlotData = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            String date = sortedDates.get(i);
            DailyData aData = assetData.get(date);
            DailyData mData = marketData.get(date);

            assetDailyData.add(aData);
            assetPrices[i] = aData.getClose();
            marketPrices[i] = mData.getClose();

            // Calculamos el True Range de esta vela para plotear
            // TR = max(H - L, |H - C_prev|, |L - C_prev|)
            if (i > 0) {
                DailyData prev = assetDailyData.get(i-1);
                double hl = aData.getHigh() - aData.getLow();
                double hcp = Math.abs(aData.getHigh() - prev.getClose());
                double lcp = Math.abs(aData.getLow() - prev.getClose());
                trueRangePlotData.add(Math.max(hl, Math.max(hcp, lcp)));
            } else {
                // Para la primera vela (no hay previa), usamos simplemente High - Low
                trueRangePlotData.add(aData.getHigh() - aData.getLow());
            }
        } // <-- La llave extraída estaba aquí, rompiendo el flujo. Ahora está bien cerrada.

        // 2. Extracción de Vectores de Retorno
        double[] assetReturns = VolatilityEngine.calculateLogReturns(assetPrices);
        double[] marketReturns = VolatilityEngine.calculateLogReturns(marketPrices);

        // 3. Cálculos del Motor Matemático
        currentTicker = ticker;
        currentATR = VolatilityEngine.calculateATR(assetDailyData, 14);
        currentRVOL = VolatilityEngine.calculateRVOL(assetDailyData, 20);
        currentPercentile = VolatilityEngine.calculateVolatilityPercentile(assetReturns, 20) * 100;
        currentBeta = VolatilityEngine.calculateBeta(assetReturns, marketReturns);

        // 4. Renderizado del HUD
        tvBeta.setText(String.format(Locale.US, "Beta (β): %.2f", currentBeta));
        tvVolPercentile.setText(String.format(Locale.US, "Percentil Volatilidad: %.1f%%", currentPercentile));
        tvATR.setText(String.format(Locale.US, "ATR (Rango Diario): $%.2f", currentATR));
        tvRVOL.setText(String.format(Locale.US, "Volumen Relativo (RVOL): %.2fx", currentRVOL));

        // Colorimetría condicional
        tvBeta.setTextColor(currentBeta > 1.2 ? Color.parseColor("#FF4444") : Color.parseColor("#00FF00"));
        tvRVOL.setTextColor(currentRVOL > 1.5 ? Color.GREEN : (currentRVOL < 0.8 ? Color.RED : Color.WHITE));

        // 5. Inyección de datos al nuevo renderizador
        renderizarGraficoVolatilidadPura(sortedDates, assetDailyData, trueRangePlotData);

        btnAnalyzeVol.setEnabled(true);
        btnAnalyzeVol.setText("ANALIZAR");
    }

private void renderizarGraficoVolatilidadPura(List<String> sortedDates, List<DailyData> assetData, List<Double> trData) {
    if (assetData.size() < 2) return;

    // Limpiamos datos previos
    volCombinedChart.clear();

    List<BarEntry> volumeEntries = new ArrayList<>();
    List<BarEntry> trEntries = new ArrayList<>(); // Barras de fondo para el ruido diario
    List<Entry> atrLineEntries = new ArrayList<>(); // Línea ATR

    // Usamos los últimos 60 días para el zoom inicial (trimestre)
    int startIndex = Math.max(0, assetData.size() - 60);
    int xIndex = 0;

    // Necesitamos calcular el ATR rodante para la línea del gráfico (no solo el escalar del HUD)
    int atrPeriod = 14;

    for (int i = startIndex; i < assetData.size(); i++) {
        DailyData d = assetData.get(i);

        // 1. Volumen (Eje Y Derecho)
        // SOLUCIÓN VISIBILIDAD: Usamos valor real, la estética la definimos luego
        volumeEntries.add(new BarEntry(xIndex, (float) d.getVolume()));

        // 2. True Range (Ruido diario) - Fondo del eje Y Izquierdo
        double currentTR = trData.get(i);
        trEntries.add(new BarEntry(xIndex, (float) currentTR));

        // 3. Línea ATR Rodante (Eje Y Izquierdo)
        if (i >= atrPeriod + startIndex) { // Esperamos a tener datos suficientes
            double sumTR = 0;
            for (int j = i - atrPeriod + 1; j <= i; j++) {
                sumTR += trData.get(j);
            }
            double currentATRLine = sumTR / atrPeriod;
            atrLineEntries.add(new Entry(xIndex, (float) currentATRLine));
        }

        xIndex++;
    }

    // --- SOLUCIÓN VISIBILIDAD VOLUMEN ---
    // Asignamos volumen al eje derecho y usamos CIAN BRILLANTE y semitransparente
    BarDataSet volSet = new BarDataSet(volumeEntries, "Volumen Transado");
    volSet.setColor(Color.parseColor("#7700FFFF")); // Cian con alpha (brillante pero deja ver)
    volSet.setDrawValues(false);
    volSet.setAxisDependency(YAxis.AxisDependency.RIGHT);

    // --- NUEVO ENFOQUE: Gráfico de Ruido de Fondo ---
    // Barras grises oscuras para el rango diario
    BarDataSet trSet = new BarDataSet(trEntries, "Rango Diario TR ($)");
    trSet.setColor(Color.parseColor("#33AAAAAA")); // Gris muy oscuro de fondo
    trSet.setDrawValues(false);
    trSet.setAxisDependency(YAxis.AxisDependency.LEFT);

    // Línea ATR que "cruza" el ruido
    LineDataSet atrSet = new LineDataSet(atrLineEntries, "ATR (Media 14d) ($)");
    atrSet.setColor(Color.parseColor("#00FFFF")); // Cian puro (brillante)
    atrSet.setLineWidth(2.5f);
    atrSet.setDrawCircles(false);
    atrSet.setDrawValues(false);
    atrSet.setAxisDependency(YAxis.AxisDependency.LEFT);

    // Agrupamos los datos
    BarData barData = new BarData(trSet, volSet); // Dos sets de barras distintos
    barData.setBarWidth(0.8f);

    LineData lineData = new LineData(atrSet);

    CombinedData combinedData = new CombinedData();
    combinedData.setData(barData);
    combinedData.setData(lineData);

    // --- SOLUCIÓN FECHAS eje X ---
    // Creamos una sublista de fechas para que coincida con nuestro zoom de 60 días
    List<String> axisDates = new ArrayList<>();
    for (int i = startIndex; i < sortedDates.size(); i++) {
        axisDates.add(sortedDates.get(i));
    }

    XAxis xAxis = volCombinedChart.getXAxis();
    // Usamos IndexAxisValueFormatter pasándole nuestra lista de fechas reales
    xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(axisDates));
    xAxis.setLabelCount(5); // Forzamos 5 etiquetas espaciadas para que no se solapen
    xAxis.setGranularity(1f); // Evitamos etiquetas repetidas en zoom

    // Actualizamos títulos de ejes
    YAxis leftAxis = volCombinedChart.getAxisLeft();
    leftAxis.setDrawGridLines(true);
    leftAxis.setGridColor(Color.parseColor("#22AAAAAA")); // Grid sutil
    volCombinedChart.getAxisRight().setDrawGridLines(false); // No ensuciamos volumen

    volCombinedChart.setData(combinedData);

    // Refrescamos y hacemos zoom
    volCombinedChart.notifyDataSetChanged();
    volCombinedChart.invalidate();
}

    private void configurarLienzo() {
        volCombinedChart.setBackgroundColor(Color.BLACK);
        volCombinedChart.getDescription().setEnabled(false);
        volCombinedChart.setDrawGridBackground(false);
        // Añadimos padding inferior para que la leyenda respire
        volCombinedChart.setExtraBottomOffset(10f);

        XAxis xAxis = volCombinedChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = volCombinedChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);

        // Eje derecho para el volumen (sin grid para no ensuciar)
        YAxis rightAxis = volCombinedChart.getAxisRight();
        rightAxis.setTextColor(Color.GRAY);
        rightAxis.setDrawGridLines(false);
        rightAxis.setSpaceTop(80f); // Achata el volumen hacia abajo (ocupa solo el 20% inferior)

        // --- NUEVO: CONFIGURACIÓN DE LA LEYENDA ---
        com.github.mikephil.charting.components.Legend legend = volCombinedChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(Color.WHITE); // Texto en blanco para contraste
        legend.setTextSize(11f);
        legend.setForm(com.github.mikephil.charting.components.Legend.LegendForm.SQUARE);
        legend.setFormSize(10f);

        // Topología de la leyenda
        legend.setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        // Activamos WordWrap por si en pantallas pequeñas no caben los 3 textos en una sola línea
        legend.setWordWrapEnabled(true);
    }

    @Override
    public String getTabName() {
        return "Análisis de Volatilidad y Liquidez";
    }

    @Override
    public String getContextualData() {
        if (currentTicker.isEmpty()) {
            return "No hay ningún activo analizado en la pestaña de volatilidad.";
        }

        return String.format(Locale.US,
                "Eres un trader institucional. Analiza el riesgo de ejecución y precio de %s. " +
                        "Beta contra S&P500: %.2f. Percentil de volatilidad (20d vs 252d): %.1f%%. " +
                        "ATR (Rango diario): $%.2f. RVOL (Volumen Relativo): %.2fx. " +
                        "Explica rigurosamente si es fácil entrar/salir ahora mismo y si el activo está actuando " +
                        "como amplificador (riesgo direccional) o refugio frente al S&P500. Máximo un párrafo.",
                currentTicker, currentBeta, currentPercentile, currentATR, currentRVOL);
    }
}
