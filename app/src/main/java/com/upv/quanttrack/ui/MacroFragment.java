package com.upv.quanttrack.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.upv.quanttrack.R;
import com.upv.quanttrack.data.macro.TreasuryRepository;
import com.upv.quanttrack.data.macro.YieldCurveData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MacroFragment extends Fragment {

    private LineChart yieldCurveChart;
    private LineChart temporalScoresChart;
    private TextView tvSpread;
    private TextView tvFpcaAnalysis;
    private TreasuryRepository repository;


    // Eje X: Índices topológicos de los vencimientos
    private final String[] labelsVencimiento = new String[]{"1M", "3M", "6M", "1Y", "2Y", "3Y", "5Y", "7Y", "10Y", "20Y", "30Y"};
    // Eje X: Fechas del FPCA
    private List<String> fechasFpca = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_macro, container, false);

        yieldCurveChart = view.findViewById(R.id.yieldCurveChart);
        tvSpread = view.findViewById(R.id.tvSpread);
        tvFpcaAnalysis = view.findViewById(R.id.tvFpcaAnalysis);
        temporalScoresChart = view.findViewById(R.id.temporalScoresChart);

        repository = new TreasuryRepository();

        configurarLienzo();
        configurarLienzoTemporal();
        descargarYRenderizarCurva();

        return view;
    }

    private void descargarYRenderizarCurva() {
        tvSpread.setText("Descargando datos del Tesoro de EE.UU...");

        repository.fetchYieldCurveMatrix(new TreasuryRepository.YieldCallback() {
            @Override
            public void onSuccess(List<YieldCurveData> historicalData) {
                if (getActivity() == null || historicalData.isEmpty()) return;

                getActivity().runOnUiThread(() -> {
                    // El índice 0 es el día más reciente
                    YieldCurveData curvaHoy = historicalData.get(0);

                    // Cálculo del Spread (10Y - 2Y)
                    double yield10Y = curvaHoy.getRate(curvaHoy.y10);
                    double yield2Y = curvaHoy.getRate(curvaHoy.y2);
                    double spread = yield10Y - yield2Y;

                    tvSpread.setText(String.format(Locale.US, "Curva actual (%s) | Spread 10Y-2Y: %.2f bps", curvaHoy.date, spread * 100));
                    tvSpread.setTextColor(spread < 0 ? Color.RED : Color.GREEN);

                    // Extraer vector espacial
                    List<Entry> puntosCurva = new ArrayList<>();
                    puntosCurva.add(new Entry(0, (float) curvaHoy.getRate(curvaHoy.m1)));
                    puntosCurva.add(new Entry(1, (float) curvaHoy.getRate(curvaHoy.m3)));
                    puntosCurva.add(new Entry(2, (float) curvaHoy.getRate(curvaHoy.m6)));
                    puntosCurva.add(new Entry(3, (float) curvaHoy.getRate(curvaHoy.y1)));
                    puntosCurva.add(new Entry(4, (float) yield2Y));
                    puntosCurva.add(new Entry(5, (float) curvaHoy.getRate(curvaHoy.y3)));
                    puntosCurva.add(new Entry(6, (float) curvaHoy.getRate(curvaHoy.y5)));
                    puntosCurva.add(new Entry(7, (float) curvaHoy.getRate(curvaHoy.y7)));
                    puntosCurva.add(new Entry(8, (float) yield10Y));
                    puntosCurva.add(new Entry(9, (float) curvaHoy.getRate(curvaHoy.y20)));
                    puntosCurva.add(new Entry(10, (float) curvaHoy.getRate(curvaHoy.y30)));

                    // Pintar la curva principal
                    LineDataSet dataSet = new LineDataSet(puntosCurva, "Yield Curve (%)");
                    dataSet.setColor(Color.CYAN);
                    dataSet.setLineWidth(3f);
                    dataSet.setCircleColor(Color.YELLOW);
                    dataSet.setCircleRadius(4f);
                    dataSet.setDrawValues(true);
                    dataSet.setValueTextColor(Color.WHITE);
                    dataSet.setValueTextSize(10f);
                    dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                    LineData lineData = new LineData(dataSet);
                    yieldCurveChart.setData(lineData);
                    yieldCurveChart.invalidate();

                    // --- 1. CONSTRUCCIÓN DEL TENSOR ---
                    int nDays = historicalData.size();
                    int nMaturities = 11;
                    double[][] yieldMatrix = new double[nDays][nMaturities];

                    for (int i = 0; i < nDays; i++) {
                        YieldCurveData day = historicalData.get(i);
                        yieldMatrix[i][0] = day.getRate(day.m1);
                        yieldMatrix[i][1] = day.getRate(day.m3);
                        yieldMatrix[i][2] = day.getRate(day.m6);
                        yieldMatrix[i][3] = day.getRate(day.y1);
                        yieldMatrix[i][4] = day.getRate(day.y2);
                        yieldMatrix[i][5] = day.getRate(day.y3);
                        yieldMatrix[i][6] = day.getRate(day.y5);
                        yieldMatrix[i][7] = day.getRate(day.y7);
                        yieldMatrix[i][8] = day.getRate(day.y10);
                        yieldMatrix[i][9] = day.getRate(day.y20);
                        yieldMatrix[i][10] = day.getRate(day.y30);
                    }

                    // --- 2. EJECUCIÓN DEL ÁLGEBRA LINEAL ---
                    tvFpcaAnalysis.setText("Calculando matriz de covarianza y autovectores...");
                    com.upv.quanttrack.domain.math.FpcaEngine engine = new com.upv.quanttrack.domain.math.FpcaEngine();
                    com.upv.quanttrack.domain.math.FpcaEngine.FpcaResult fpcaResult = engine.decomposeYieldCurve(yieldMatrix);

                    // --- 3. SALIDA AL HUD ---
                    String analysis = String.format(java.util.Locale.US,
                            "Descomposición Eigen (Varianza Explicada):\n" +
                                    "PC1 (Nivel): %.2f%%\n" +
                                    "PC2 (Pendiente): %.2f%%\n" +
                                    "PC3 (Curvatura): %.2f%%\n" +
                                    "Suma Total: %.2f%%",
                            fpcaResult.varPc1, fpcaResult.varPc2, fpcaResult.varPc3,
                            (fpcaResult.varPc1 + fpcaResult.varPc2 + fpcaResult.varPc3));
                    tvFpcaAnalysis.setText(analysis);

                    // --- 4. GRAFICAR LA SERIE TEMPORAL (SCORES) ---
                    fechasFpca.clear();
                    List<Entry> entriesPC1 = new ArrayList<>();
                    List<Entry> entriesPC2 = new ArrayList<>();
                    List<Entry> entriesPC3 = new ArrayList<>();

                    for (int i = 0; i < nDays; i++) {
                        int timeIndex = nDays - 1 - i; // Del pasado al presente

                        // Guardamos la fecha correspondiente a este punto del eje X
                        fechasFpca.add(historicalData.get(timeIndex).date);

                        entriesPC1.add(new Entry(i, (float) fpcaResult.pc1Scores[timeIndex]));
                        entriesPC2.add(new Entry(i, (float) fpcaResult.pc2Scores[timeIndex]));
                        entriesPC3.add(new Entry(i, (float) fpcaResult.pc3Scores[timeIndex]));
                    }


                    LineDataSet setPC1 = new LineDataSet(entriesPC1, "Nivel (PC1)");
                    setPC1.setColor(Color.CYAN);
                    setPC1.setDrawCircles(false);
                    setPC1.setLineWidth(2f);

                    LineDataSet setPC2 = new LineDataSet(entriesPC2, "Pendiente (PC2)");
                    setPC2.setColor(Color.MAGENTA);
                    setPC2.setDrawCircles(false);
                    setPC2.setLineWidth(2f);

                    LineDataSet setPC3 = new LineDataSet(entriesPC3, "Curvatura (PC3)");
                    setPC3.setColor(Color.YELLOW);
                    setPC3.setDrawCircles(false);
                    setPC3.setLineWidth(2f);

                    LineData scoresData = new LineData(setPC1, setPC2, setPC3);
                    temporalScoresChart.setData(scoresData);


// REFUERZO: Obligamos al eje a recalcularse con los nuevos datos de la lista
                    temporalScoresChart.getXAxis().setLabelCount(6, false);
                    temporalScoresChart.notifyDataSetChanged(); // Notifica cambios en los datos
                    temporalScoresChart.invalidate(); // Refresca el dibujo
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    tvSpread.setText("Error: " + error);
                    tvSpread.setTextColor(Color.RED);
                    Toast.makeText(getContext(), "Error en API del Tesoro", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void configurarLienzo() {
        yieldCurveChart.setBackgroundColor(Color.BLACK);
        yieldCurveChart.getDescription().setEnabled(false);
        yieldCurveChart.getLegend().setTextColor(Color.WHITE);
        yieldCurveChart.setExtraOffsets(10f, 10f, 10f, 10f);

        XAxis xAxis = yieldCurveChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < labelsVencimiento.length) {
                    return labelsVencimiento[index];
                }
                return "";
            }
        });

        YAxis leftAxis = yieldCurveChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.DKGRAY);

        yieldCurveChart.getAxisRight().setEnabled(false);
    }

    private void configurarLienzoTemporal() {
        temporalScoresChart.setBackgroundColor(Color.BLACK);
        temporalScoresChart.getDescription().setEnabled(false);
        temporalScoresChart.getLegend().setTextColor(Color.WHITE);

        XAxis xAxis = temporalScoresChart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true); // Fuerza a que no se salte índices
        xAxis.setLabelCount(6, false);
        xAxis.setLabelRotationAngle(-45f);

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                int index = (int) value;
                // IMPORTANTE: Comprobamos el tamaño de la lista dinámicamente
                if (index >= 0 && index < fechasFpca.size()) {
                    String fullDate = fechasFpca.get(index);
                    String[] parts = fullDate.split("-");
                    if (parts.length >= 3) {
                        return parts[2] + "/" + parts[1];
                    }
                    return fullDate;
                }
                return "";
            }
        });

        YAxis leftAxis = temporalScoresChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.DKGRAY);
        temporalScoresChart.getAxisRight().setEnabled(false);
    }
}