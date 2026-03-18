package com.upv.quanttrack.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
import com.upv.quanttrack.data.llm.LlmRepository;
import com.upv.quanttrack.domain.math.SimpleMovingAverage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private CombinedChart combinedChart;
    private EditText etTicker;
    private Button btnSearch;
    private TextView tvCurrentPrice; // El HUD Financiero
    private MarketRepository repository;
    private TextView tvLlmAnalysis;
    private LlmRepository llmRepository;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Enlazamos XML
        combinedChart = findViewById(R.id.combinedChart);
        etTicker = findViewById(R.id.etTicker);
        btnSearch = findViewById(R.id.btnSearch);
        tvCurrentPrice = findViewById(R.id.tvCurrentPrice);
        tvLlmAnalysis = findViewById(R.id.tvLlmAnalysis);
        llmRepository = new LlmRepository();
        repository = new MarketRepository();

        configurarEstiloGrafico();

        // 2. Evento del botón
        btnSearch.setOnClickListener(v -> {
            String ticker = etTicker.getText().toString().trim().toUpperCase();
            if (!ticker.isEmpty()) {
                fetchAndProcess(ticker);
            }
        });

        fetchAndProcess("AAPL");
    }

    private void fetchAndProcess(String ticker) {
        // Estado visual de carga
        tvCurrentPrice.setText("Descargando " + ticker + "...");
        tvCurrentPrice.setTextColor(Color.YELLOW);

        repository.fetchDailyData(ticker, new MarketRepository.DataCallback() {
            @Override
            public void onSuccess(Map<String, DailyData> data) {
                renderizarTodo(data, ticker);
            }

            @Override
            public void onError(String error) {
                Log.e("QUANT_TRACK_UI", "Error: " + error);

                // Limpieza de seguridad en caso de error (ticker inventado o sin internet)
                combinedChart.clear();
                combinedChart.invalidate();
                tvCurrentPrice.setText("Error / Sin Datos");
                tvCurrentPrice.setTextColor(Color.RED);
                Toast.makeText(MainActivity.this, "Error de red o ticker inválido", Toast.LENGTH_SHORT).show();
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

        for (int i = 0; i < n; i++) {
            DailyData d = data.get(fechas.get(i));
            closePrices[i] = d.getClose();
            candleEntries.add(new CandleEntry(i, (float)d.getHigh(), (float)d.getLow(), (float)d.getOpen(), (float)d.getClose()));
        }

        // --- HUD FINANCIERO (Precio Actual) ---
        double ultimoPrecio = closePrices[n - 1];
        String ultimaFecha = fechas.get(n - 1);
        tvCurrentPrice.setText(ticker + " | " + ultimaFecha + " | $" + String.format("%.2f", ultimoPrecio));

        if (n > 1 && ultimoPrecio >= closePrices[n - 2]) {
            tvCurrentPrice.setTextColor(Color.GREEN);
        } else {
            tvCurrentPrice.setTextColor(Color.WHITE); // Blanco o rojo si cae, lo dejamos neutro por elegancia
        }

        // --- MOTOR MATEMÁTICO ---
        SimpleMovingAverage sma20Calc = new SimpleMovingAverage(20);
        SimpleMovingAverage sma50Calc = new SimpleMovingAverage(50);
        SimpleMovingAverage sma200Calc = new SimpleMovingAverage(200);

        double[] sma20Results = sma20Calc.calculate(closePrices);
        double[] sma50Results = sma50Calc.calculate(closePrices);
        double[] sma200Results = sma200Calc.calculate(closePrices);

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

        // --- RENDERIZADO GRÁFICO ---
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

        // --- TRADUCCIÓN DEL EJE X (Índice -> Fecha) ---
        XAxis xAxis = combinedChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < fechas.size()) {
                    return fechas.get(index).substring(5); // Devuelve solo "MM-DD"
                }
                return "";
            }
        });
        xAxis.setLabelRotationAngle(-45f);

        combinedChart.setVisibleXRangeMaximum(150);
        combinedChart.moveViewToX(n);
        combinedChart.invalidate();
        // --- INYECCIÓN AL LLM (PROMPT ENGINEERING) ---

        // Indicador de carga
        tvLlmAnalysis.setText("Analizando topología y medias móviles con IA...");
        tvLlmAnalysis.setTextColor(Color.YELLOW);

        // 1. Extraer los últimos escalares calculados
        double ultimaSma20 = sma20Results.length > 0 ? sma20Results[n - 1] : 0;
        double ultimaSma50 = sma50Results.length > 0 ? sma50Results[n - 1] : 0;
        double ultimaSma200 = sma200Results.length > 0 ? sma200Results[n - 1] : 0;

        // 2. Construir el Prompt determinista
        String prompt = String.format(
                "Eres un analista cuantitativo riguroso. Analiza la acción %s. " +
                        "Precio de cierre de hoy: %.2f. " +
                        "Media Móvil 20 días: %.2f. " +
                        "Media Móvil 50 días: %.2f. " +
                        "Media Móvil 200 días: %.2f. " +
                        "Instrucciones: Evalúa la tendencia actual comparando el precio con estas medias. " +
                        "¿Hay soporte o resistencia? ¿Es un régimen alcista o bajista? " +
                        "Sé directo, usa lenguaje técnico financiero y limítate a un párrafo conciso. No hagas saludos.",
                ticker, ultimoPrecio, ultimaSma20, ultimaSma50, ultimaSma200
        );

        // 3. Ejecutar la red neuronal
        llmRepository.analyzeMarket(prompt, new LlmRepository.LlmCallback() {
            @Override
            public void onSuccess(String analysis) {
                tvLlmAnalysis.setText(analysis);
                tvLlmAnalysis.setTextColor(Color.WHITE);
            }

            @Override
            public void onError(String error) {
                tvLlmAnalysis.setText("Error al generar análisis: " + error);
                tvLlmAnalysis.setTextColor(Color.RED);
            }
        });
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
}