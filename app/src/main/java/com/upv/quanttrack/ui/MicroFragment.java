package com.upv.quanttrack.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.upv.quanttrack.R;
import com.upv.quanttrack.data.DailyData;
import com.upv.quanttrack.data.MarketRepository;
import com.upv.quanttrack.domain.math.SimpleMovingAverage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;



// 1. IMPLEMENTAMOS LA INTERFAZ
public class MicroFragment extends Fragment implements Analyzable {

    private CombinedChart combinedChart;
    private EditText etTicker;
    private Button btnSearch;
    private TextView tvCurrentPrice;
    private MarketRepository repository;
    private com.github.mikephil.charting.charts.LineChart rsiChart;

    // NOTA: Hemos eliminado tvLlmAnalysis y llmRepository de aquí.

    // 2. VARIABLES DE ESTADO PARA LA IA
    private String currentTicker = "";
    private double currentPrice = 0;
    private double currentSma20 = 0, currentSma50 = 0, currentSma200 = 0;
    private double currentRsi = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflamos el XML de este fragmento específico
        View view = inflater.inflate(R.layout.fragment_micro, container, false);

        // Enlazamos las vistas desde el 'view' padre
        combinedChart = view.findViewById(R.id.combinedChart);
        etTicker = view.findViewById(R.id.etTicker);
        btnSearch = view.findViewById(R.id.btnSearch);
        tvCurrentPrice = view.findViewById(R.id.tvCurrentPrice);
        rsiChart = view.findViewById(R.id.rsiChart);
        configurarLienzoRSI(); // Nuevo método de diseño
        repository = new MarketRepository();

        configurarEstiloGrafico();

        btnSearch.setOnClickListener(v -> {
            String ticker = etTicker.getText().toString().trim().toUpperCase();
            if (!ticker.isEmpty()) {
                fetchAndProcess(ticker);
            }
        });

        // Cargamos una acción por defecto al abrir la pestaña
        fetchAndProcess("AAPL");

        return view;
    }

    private void fetchAndProcess(String ticker) {
        tvCurrentPrice.setText("Descargando " + ticker + "...");
        tvCurrentPrice.setTextColor(Color.YELLOW);

        repository.fetchDailyData(ticker, new MarketRepository.DataCallback() {
            @Override
            public void onSuccess(Map<String, DailyData> data) {
                if (getActivity() == null) return; // Defensa: si el usuario cambió de pestaña rápido, abortamos
                getActivity().runOnUiThread(() -> renderizarTodo(data, ticker));
            }

            @Override
            public void onError(String error) {
                Log.e("QUANT_TRACK_UI", "Error: " + error);
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    combinedChart.clear();
                    combinedChart.invalidate();
                    tvCurrentPrice.setText("Error / Sin Datos");
                    tvCurrentPrice.setTextColor(Color.RED);
                    Toast.makeText(getContext(), "Error de red o ticker inválido", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void renderizarTodo(Map<String, DailyData> data, String ticker) {
        if (data == null || data.isEmpty()) {
            tvCurrentPrice.setText("Sin datos para " + ticker);
            tvCurrentPrice.setTextColor(Color.RED);
            return;
        }

        combinedChart.clear();

        List<String> fechas = new ArrayList<>(data.keySet());
        Collections.sort(fechas);

        int n = fechas.size();
        List<CandleEntry> candleEntries = new ArrayList<>();
        double[] closePrices = new double[n];

        List<DailyData> assetDailyData = new ArrayList<>(); // <-- NUEVO: Lista para el motor RSI

        for (int i = 0; i < n; i++) {
            DailyData d = data.get(fechas.get(i));
            assetDailyData.add(d); // <-- NUEVO: Guardamos el objeto completo

            closePrices[i] = d.getClose();
            candleEntries.add(new CandleEntry(i, (float)d.getHigh(), (float)d.getLow(), (float)d.getOpen(), (float)d.getClose()));
        }

        double ultimoPrecio = closePrices[n - 1];
        String ultimaFecha = fechas.get(n - 1);
        tvCurrentPrice.setText(ticker + " | " + ultimaFecha + " | $" + String.format(Locale.US, "%.2f", ultimoPrecio));

        if (n > 1 && ultimoPrecio >= closePrices[n - 2]) {
            tvCurrentPrice.setTextColor(Color.GREEN);
        } else {
            tvCurrentPrice.setTextColor(Color.WHITE);
        }

        SimpleMovingAverage sma20Calc = new SimpleMovingAverage(20);
        SimpleMovingAverage sma50Calc = new SimpleMovingAverage(50);
        SimpleMovingAverage sma200Calc = new SimpleMovingAverage(200);

        double[] sma20Results = sma20Calc.calculate(closePrices);
        double[] sma50Results = sma50Calc.calculate(closePrices);
        double[] sma200Results = sma200Calc.calculate(closePrices);

        // 3. ACTUALIZAMOS EL ESTADO PARA CUANDO SE INVOQUE LA IA
        currentTicker = ticker;
        currentPrice = ultimoPrecio;
        currentSma20 = sma20Results.length > 0 ? sma20Results[n - 1] : 0;
        currentSma50 = sma50Results.length > 0 ? sma50Results[n - 1] : 0;
        currentSma200 = sma200Results.length > 0 ? sma200Results[n - 1] : 0;

        List<Entry> lineEntries20 = new ArrayList<>();
        List<Entry> lineEntries50 = new ArrayList<>();
        List<Entry> lineEntries200 = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            if (sma20Results.length > i && !Double.isNaN(sma20Results[i])) {
                lineEntries20.add(new Entry(i, (float) sma20Results[i]));
            }
            if (sma50Results.length > i && !Double.isNaN(sma50Results[i])) {
                lineEntries50.add(new Entry(i, (float) sma50Results[i]));
            }
            if (sma200Results.length > i && !Double.isNaN(sma200Results[i])) {
                lineEntries200.add(new Entry(i, (float) sma200Results[i]));
            }
        }

        CandleDataSet candleSet = new CandleDataSet(candleEntries, "Precio");
        candleSet.setDecreasingColor(Color.RED);
        candleSet.setIncreasingColor(Color.GREEN);
        candleSet.setShadowColor(Color.LTGRAY);
        candleSet.setDrawValues(false);

        LineData lineData = new LineData();

        if (!lineEntries20.isEmpty()) {
            LineDataSet set20 = new LineDataSet(lineEntries20, "SMA 20");
            set20.setColor(Color.CYAN);
            set20.setLineWidth(1.5f);
            set20.setDrawCircles(false);
            set20.setDrawValues(false);
            lineData.addDataSet(set20);
        }

        if (!lineEntries50.isEmpty()) {
            LineDataSet set50 = new LineDataSet(lineEntries50, "SMA 50");
            set50.setColor(Color.MAGENTA);
            set50.setLineWidth(2f);
            set50.setDrawCircles(false);
            set50.setDrawValues(false);
            lineData.addDataSet(set50);
        }

        if (!lineEntries200.isEmpty()) {
            LineDataSet set200 = new LineDataSet(lineEntries200, "SMA 200");
            set200.setColor(Color.YELLOW);
            set200.setLineWidth(2.5f);
            set200.setDrawCircles(false);
            set200.setDrawValues(false);
            lineData.addDataSet(set200);
        }

        CombinedData combinedData = new CombinedData();
        combinedData.setData(new CandleData(candleSet));

        if (lineData.getDataSetCount() > 0) {
            combinedData.setData(lineData);
        }

        combinedChart.setData(combinedData);

        XAxis xAxis = combinedChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < fechas.size()) {
                    return fechas.get(index).substring(5);
                }
                return "";
            }
        });
        xAxis.setLabelRotationAngle(-45f);

        combinedChart.setVisibleXRangeMaximum(150);
        combinedChart.moveViewToX(n);
        combinedChart.invalidate();

        // El bloque automático de LLM que había aquí se ha eliminado.
        List<Double> rsiValues = com.upv.quanttrack.domain.math.OscillatorEngine.calculateRSI(assetDailyData, 14);
        List<Entry> rsiEntries = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            double rsiVal = rsiValues.get(i);
            if (rsiVal > 0) { // Omitimos los primeros 14 días nulos de carga
                rsiEntries.add(new Entry(i, (float) rsiVal));
            }
        }

        // Guardamos el último RSI para el prompt de la IA
        currentRsi = rsiValues.isEmpty() ? 0 : rsiValues.get(rsiValues.size() - 1);

        LineDataSet rsiDataSet = new LineDataSet(rsiEntries, "RSI (14)");
        rsiDataSet.setColor(Color.parseColor("#BB86FC")); // Morado técnico
        rsiDataSet.setLineWidth(1.5f);
        rsiDataSet.setDrawCircles(false);
        rsiDataSet.setDrawValues(false);

        LineData rsiData = new LineData(rsiDataSet);
        rsiChart.setData(rsiData);

        // Sincronizamos el zoom y el paneo para que coincida exactamente con las velas de arriba
        rsiChart.setVisibleXRangeMaximum(150);
        rsiChart.moveViewToX(n);
        rsiChart.invalidate();
    }

    private void configurarEstiloGrafico() {
        combinedChart.setBackgroundColor(Color.BLACK);
        XAxis xAxis = combinedChart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        combinedChart.getAxisLeft().setTextColor(Color.WHITE);
        combinedChart.getAxisRight().setEnabled(false);
        combinedChart.getLegend().setTextColor(Color.WHITE);
        combinedChart.getDescription().setEnabled(false);
    }

    // 4. MÉTODOS DE LA INTERFAZ ANALYZABLE
    @Override
    public String getTabName() {
        return "Microeconomía (Acciones)";
    }

    @Override
    public String getContextualData() {
        if (currentTicker.isEmpty() || currentPrice == 0) {
            return "Aún no hay datos cargados para analizar.";
        }

        return String.format(Locale.US,
                "Eres un analista cuantitativo riguroso. Analiza la acción %s. " +
                        "Precio de cierre actual: %.2f. " +
                        "RSI (14 días): %.2f. " +
                        "Media Móvil 20 días: %.2f. " +
                        "Media Móvil 50 días: %.2f. " +
                        "Media Móvil 200 días: %.2f. " +
                        "Instrucciones: Evalúa la tendencia actual comparando el precio con estas medias. " +
                        "¿Hay soporte o resistencia? Luego, usa el RSI para determinar si el movimiento está sobrecomprado (>70) o sobrevendido (<30) y si tiene fuerza real. " +
                        "Sé directo, usa lenguaje que cualquiera pueda entender y limítate a un párrafo conciso estructurado. No hagas saludos.",
                currentTicker, currentPrice, currentRsi, currentSma20, currentSma50, currentSma200
        );
    }
    private void configurarLienzoRSI() {
        rsiChart.setBackgroundColor(android.graphics.Color.BLACK);

        // 1. Identificación del Indicador (Ahora sí será visible)
        com.github.mikephil.charting.components.Description desc = new com.github.mikephil.charting.components.Description();
        desc.setText("RELATIVE STRENGTH INDEX (14)");
        desc.setTextColor(android.graphics.Color.GRAY);
        desc.setTextSize(9f);
        // Lo posicionamos un poco desplazado del borde
        desc.setXOffset(10f);
        desc.setYOffset(10f);
        rsiChart.setDescription(desc);
        rsiChart.getDescription().setEnabled(true);

        rsiChart.getLegend().setEnabled(false);

        // 2. Eje X: Invisible pero sincronizado
        com.github.mikephil.charting.components.XAxis xAxis = rsiChart.getXAxis();
        xAxis.setEnabled(false);
        xAxis.setAxisMinimum(0f); // Evita que el gráfico "baile" respecto al de arriba

        // 3. Eje Y: Limpieza y niveles de referencia
        com.github.mikephil.charting.components.YAxis leftAxis = rsiChart.getAxisLeft();
        leftAxis.setTextColor(android.graphics.Color.LTGRAY);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(false);

        // Solo mostramos 3 etiquetas: 0, 50 y 100 para no ensuciar la vista
        leftAxis.setLabelCount(3, true);

        // 4. Líneas de Límite (Sobrecompra / Sobreventa)
        // Limpiamos líneas previas para evitar duplicados en cada búsqueda
        leftAxis.removeAllLimitLines();

        com.github.mikephil.charting.components.LimitLine upperLine = new com.github.mikephil.charting.components.LimitLine(70f, "70 - OB");
        upperLine.setLineColor(android.graphics.Color.RED);
        upperLine.setLineWidth(0.8f);
        upperLine.enableDashedLine(10f, 10f, 0f);
        upperLine.setTextColor(android.graphics.Color.RED);
        upperLine.setTextSize(7f);
        leftAxis.addLimitLine(upperLine);

        com.github.mikephil.charting.components.LimitLine lowerLine = new com.github.mikephil.charting.components.LimitLine(30f, "30 - OS");
        lowerLine.setLineColor(android.graphics.Color.GREEN);
        lowerLine.setLineWidth(0.8f);
        lowerLine.enableDashedLine(10f, 10f, 0f);
        lowerLine.setTextColor(android.graphics.Color.GREEN);
        lowerLine.setTextSize(7f);
        leftAxis.addLimitLine(lowerLine);

        // Añadimos una línea neutra en 50 (opcional, ayuda visualmente)
        com.github.mikephil.charting.components.LimitLine midLine = new com.github.mikephil.charting.components.LimitLine(50f, "");
        midLine.setLineColor(android.graphics.Color.DKGRAY);
        midLine.setLineWidth(0.5f);
        leftAxis.addLimitLine(midLine);

        rsiChart.getAxisRight().setEnabled(false);
    }
}