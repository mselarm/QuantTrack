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

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.upv.quanttrack.R;
import com.upv.quanttrack.data.DailyData;
import com.upv.quanttrack.data.MarketRepository;
import com.upv.quanttrack.domain.math.RiskEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.upv.quanttrack.domain.portfolio.AppDatabase;
import com.upv.quanttrack.domain.portfolio.PortfolioAsset;
public class RiskFragment extends Fragment implements Analyzable {

    private EditText etRiskTicker, etRiskShares;
    private Button btnAddAsset, btnClearPortfolio, btnCalculateRisk;
    private TextView tvPortfolioHoldings, tvTotalValue, tvRiskMetrics;
    private PieChart portfolioPieChart;

    private final Map<String, Double> portfolio = new HashMap<>();
    private final MarketRepository repository = new MarketRepository();
    private final RiskEngine riskEngine = new RiskEngine();

    private double currentNotional = 0.0;
    private double currentVolatility = 0.0;
    private double currentVaR = 0.0;
    private TextView tvExpectedShortfall;
    private double currentCVaR = 0.0;
    private android.widget.GridLayout gridCorrelation;
    private AppDatabase db;
    private final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_risk, container, false);

        etRiskTicker = view.findViewById(R.id.etRiskTicker);
        etRiskShares = view.findViewById(R.id.etRiskShares);
        btnAddAsset = view.findViewById(R.id.btnAddAsset);
        btnClearPortfolio = view.findViewById(R.id.btnClearPortfolio);
        btnCalculateRisk = view.findViewById(R.id.btnCalculateRisk);
        tvPortfolioHoldings = view.findViewById(R.id.tvPortfolioHoldings);
        tvTotalValue = view.findViewById(R.id.tvTotalValue);
        tvRiskMetrics = view.findViewById(R.id.tvRiskMetrics);
        portfolioPieChart = view.findViewById(R.id.portfolioPieChart);
        tvExpectedShortfall = view.findViewById(R.id.tvExpectedShortfall);
        gridCorrelation = view.findViewById(R.id.gridCorrelation);
        configurarLienzoCircular();
        // 1. Instanciamos la conexión a disco
        db = AppDatabase.getDatabase(getContext());
        // 2. Cargamos el vector de estado guardado
        cargarCarteraDesdeDisco();
        btnAddAsset.setOnClickListener(v -> agregarActivo());
        btnClearPortfolio.setOnClickListener(v -> limpiarCartera());
        btnCalculateRisk.setOnClickListener(v -> evaluarRiesgo());

        return view;
    }
    private void cargarCarteraDesdeDisco() {
        diskExecutor.execute(() -> {
            // HILO I/O: Lectura
            List<PortfolioAsset> guardados = db.portfolioDao().getAllAssets();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // HILO MAIN: Inyección en RAM y renderizado
                    for (PortfolioAsset asset : guardados) {
                        portfolio.put(asset.getTicker(), asset.getShares());
                    }
                    actualizarEstadoUI();
                });
            }
        });
    }
    private void agregarActivo() {
        String ticker = etRiskTicker.getText().toString().trim().toUpperCase();
        String sharesStr = etRiskShares.getText().toString().trim();

        if (ticker.isEmpty() || sharesStr.isEmpty()) return;

        try {
            double shares = Double.parseDouble(sharesStr);
            if (shares <= 0) throw new NumberFormatException();

            // 1. Bloqueamos la interfaz visualmente para que el usuario sepa que estamos validando
            btnAddAsset.setEnabled(false);
            btnAddAsset.setText("...");

            // 2. Disparamos la validación contra la API
            repository.fetchDailyData(ticker, new MarketRepository.DataCallback() {
                @Override
                public void onSuccess(Map<String, DailyData> data) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        // El ticker es válido. Lo añadimos a la matriz matemática.
                        portfolio.put(ticker, portfolio.getOrDefault(ticker, 0.0) + shares);
                        double finalShares = portfolio.get(ticker);
                        diskExecutor.execute(() -> db.portfolioDao().insertAsset(new PortfolioAsset(ticker, finalShares)));
                        // Limpiamos los inputs y restauramos el botón
                        etRiskTicker.setText("");
                        etRiskShares.setText("");
                        btnAddAsset.setEnabled(true);
                        btnAddAsset.setText("ADD");

                        actualizarEstadoUI();
                        Toast.makeText(getContext(), ticker + " añadido a la cartera", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        // El ticker NO existe o falló la red. Restauramos el botón y mostramos el error limpio.
                        btnAddAsset.setEnabled(true);
                        btnAddAsset.setText("ADD");
                        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    });
                }
            });

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "La cantidad debe ser un número válido", Toast.LENGTH_SHORT).show();
        }
    }

    private void limpiarCartera() {
        portfolio.clear();
        currentNotional = 0.0;
        currentVolatility = 0.0;
        currentVaR = 0.0;
        actualizarEstadoUI();
        diskExecutor.execute(() -> db.portfolioDao().clearPortfolio());
    }

    private void actualizarEstadoUI() {
        if (portfolio.isEmpty()) {
            // 1. Limpieza de textos y gráfico
            tvPortfolioHoldings.setText("Cartera vacía.");
            portfolioPieChart.clear();
            portfolioPieChart.invalidate();

            // 2. Limpieza de métricas
            tvTotalValue.setText("NOTIONAL: $0.00");
            tvRiskMetrics.setText("Volatilidad Anual: --% | VaR Diario (95%): $--");
            tvExpectedShortfall.setText("CVaR (Expected Shortfall): $--");

            // 3. Destrucción de la matriz de correlación
            if (gridCorrelation != null) {
                gridCorrelation.removeAllViews();
            }
            return;
        }

        // Si hay datos, renderizamos el vector de estado en texto
        StringBuilder holdingsText = new StringBuilder("Vector de estado: ");
        for (Map.Entry<String, Double> entry : portfolio.entrySet()) {
            holdingsText.append("[").append(entry.getKey()).append(": ").append(entry.getValue()).append("] ");
        }
        tvPortfolioHoldings.setText(holdingsText.toString());
    }

    // --- EL CEREBRO DE LA OPERACIÓN ---
    private void evaluarRiesgo() {
        if (portfolio.size() < 2) {
            Toast.makeText(getContext(), "Necesitas al menos 2 activos para la matriz de covarianza", Toast.LENGTH_LONG).show();
            return;
        }

        tvRiskMetrics.setText("Descargando tensores financieros...");
        tvRiskMetrics.setTextColor(Color.YELLOW);

        // Mapa concurrente porque recibiremos callbacks desde distintos hilos de red
        Map<String, Map<String, DailyData>> allMarketData = new ConcurrentHashMap<>();
        int expectedCallbacks = portfolio.size();
        AtomicInteger completedCallbacks = new AtomicInteger(0);

        for (String ticker : portfolio.keySet()) {
            repository.fetchDailyData(ticker, new MarketRepository.DataCallback() {
                @Override
                public void onSuccess(Map<String, DailyData> data) {
                    allMarketData.put(ticker, data);
                    checkBarrier(completedCallbacks.incrementAndGet(), expectedCallbacks, allMarketData);
                }

                @Override
                public void onError(String error) {
                    // 1. Eliminamos de RAM
                    portfolio.remove(ticker);

                    // 2. Eliminamos de Disco (por si el ticker dejó de cotizar)
                    diskExecutor.execute(() -> db.portfolioDao().deleteAsset(ticker));

                    completedCallbacks.incrementAndGet();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Ticker descartado: " + ticker + " (" + error + ")", Toast.LENGTH_LONG).show();
                            // 2. Actualizamos la interfaz para que el usuario vea que ha desaparecido
                            actualizarEstadoUI();
                        });
                    }
                    checkBarrier(completedCallbacks.get(), expectedCallbacks, allMarketData);
                }
            });
        }
    }

    private void checkBarrier(int completed, int expected, Map<String, Map<String, DailyData>> allMarketData) {
        if (completed == expected) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> procesarAlgebraLineal(allMarketData));
            }
        }
    }

    private void procesarAlgebraLineal(Map<String, Map<String, DailyData>> allMarketData) {
        if (allMarketData.size() < 2) {
            tvRiskMetrics.setText("Error crítico de datos. Imposible calcular covarianza.");
            tvRiskMetrics.setTextColor(Color.RED);
            return;
        }

        // 1. Intersección de Conjuntos (Alineamiento de fechas)
        Set<String> commonDates = null;
        for (Map<String, DailyData> dataMap : allMarketData.values()) {
            if (commonDates == null) {
                commonDates = new HashSet<>(dataMap.keySet());
            } else {
                commonDates.retainAll(dataMap.keySet());
            }
        }

        if (commonDates == null || commonDates.size() < 2) {
            tvRiskMetrics.setText("No hay solapamiento temporal suficiente.");
            tvRiskMetrics.setTextColor(Color.RED);
            return;
        }

        List<String> sortedDates = new ArrayList<>(commonDates);
        Collections.sort(sortedDates);

        int nDays = sortedDates.size();
        int mAssets = allMarketData.size();
        List<String> assetList = new ArrayList<>(allMarketData.keySet());

        // 2. Cálculo de Pesos y Notional
        double totalNotional = 0.0;
        double[] pricesToday = new double[mAssets];
        String lastDate = sortedDates.get(nDays - 1);

        for (int j = 0; j < mAssets; j++) {
            String ticker = assetList.get(j);
            double shares = portfolio.get(ticker);
            double price = allMarketData.get(ticker).get(lastDate).getClose();
            pricesToday[j] = price;
            totalNotional += price * shares;
        }

        double[] weights = new double[mAssets];
        for (int j = 0; j < mAssets; j++) {
            String ticker = assetList.get(j);
            weights[j] = (pricesToday[j] * portfolio.get(ticker)) / totalNotional;
        }

        // 3. Matriz de Retornos Logarítmicos
        double[][] returnsMatrix = new double[nDays - 1][mAssets];
        for (int j = 0; j < mAssets; j++) {
            String ticker = assetList.get(j);
            Map<String, DailyData> assetData = allMarketData.get(ticker);
            for (int i = 1; i < nDays; i++) {
                double p_t = assetData.get(sortedDates.get(i)).getClose();
                double p_t_minus_1 = assetData.get(sortedDates.get(i - 1)).getClose();
                returnsMatrix[i - 1][j] = Math.log(p_t / p_t_minus_1);
            }
        }

        // 4. Inyección al Motor de Riesgo
        RiskEngine.RiskResult result = riskEngine.calculatePortfolioRisk(returnsMatrix, weights, totalNotional);

        currentNotional = totalNotional;
        currentVolatility = result.annualizedVolatility * 100;
        currentVaR = result.var95;
        currentCVaR = result.cVar95;

        // 5. Renderizado del HUD (Escalares)
        tvTotalValue.setText(String.format(Locale.US, "NOTIONAL: $%.2f", currentNotional));
        tvRiskMetrics.setText(String.format(Locale.US, "Volatilidad Anual: %.2f%% | VaR (95%%): $%.2f", currentVolatility, currentVaR));
        tvRiskMetrics.setTextColor(Color.parseColor("#FFaa00"));
        tvExpectedShortfall.setText(String.format(Locale.US, "CVaR (Expected Shortfall): $%.2f", currentCVaR));

        // 6. Actualizamos el gráfico circular (Topología 1D de pesos)
        List<PieEntry> pieEntries = new ArrayList<>();
        for (int j = 0; j < mAssets; j++) {
            pieEntries.add(new PieEntry((float) (weights[j] * 100), assetList.get(j)));
        }

        PieDataSet dataSet = new PieDataSet(pieEntries, "");

        // Concatenamos múltiples paletas para evitar repeticiones en carteras grandes
        ArrayList<Integer> coloresGrafico = new ArrayList<>();
        for (int c : ColorTemplate.MATERIAL_COLORS) coloresGrafico.add(c);
        for (int c : ColorTemplate.VORDIPLOM_COLORS) coloresGrafico.add(c);
        for (int c : ColorTemplate.JOYFUL_COLORS) coloresGrafico.add(c);
        for (int c : ColorTemplate.LIBERTY_COLORS) coloresGrafico.add(c);
        for (int c : ColorTemplate.PASTEL_COLORS) coloresGrafico.add(c);
        coloresGrafico.add(ColorTemplate.getHoloBlue());

        dataSet.setColors(coloresGrafico); // Inyectamos la super-paleta
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);
        dataSet.setSliceSpace(2f);

        portfolioPieChart.setData(new PieData(dataSet));
        portfolioPieChart.setCenterText("CAPITAL\n$" + String.format(Locale.US, "%.0f", totalNotional));
        portfolioPieChart.notifyDataSetChanged();
        portfolioPieChart.invalidate();

        // 7. Renderizado del Heatmap (Topología NxN de correlación)
        renderizarHeatmapCorrelacion(result.correlationMatrix, assetList);
    }

    private void configurarLienzoCircular() {
        portfolioPieChart.setBackgroundColor(Color.BLACK);
        portfolioPieChart.getDescription().setEnabled(false);
        portfolioPieChart.getLegend().setTextColor(Color.WHITE);
        portfolioPieChart.setDrawHoleEnabled(true);
        portfolioPieChart.setHoleColor(Color.BLACK);
        portfolioPieChart.setTransparentCircleRadius(55f);
        portfolioPieChart.setCenterText("PESOS (w)");
        portfolioPieChart.setCenterTextColor(Color.WHITE);
    }

    @Override
    public String getTabName() {
        return "Gestión de Riesgos (VaR y Markowitz)";
    }

    @Override
    public String getContextualData() {
        if (portfolio.isEmpty() || currentNotional == 0) {
            return "La cartera no ha sido analizada o está vacía.";
        }

        StringBuilder composition = new StringBuilder();
        for (Map.Entry<String, Double> entry : portfolio.entrySet()) {
            composition.append(entry.getKey()).append(" (").append(entry.getValue()).append(" uds), ");
        }

        return String.format(Locale.US,
                "Eres un gestor de riesgos cuantitativo. Analiza esta cartera: %s. " +
                        "Notional Total: $%.2f. Volatilidad Anualizada: %.2f%%. VaR Diario (95%%): $%.2f. CVaR (Expected Shortfall): $%.2f. " +
                        "Instrucciones: Evalúa el riesgo de concentración y el riesgo de cola (diferencia entre VaR y CVaR). " +
                        "Sugiere activos ortogonales para optimizar la frontera de Markowitz. " +
                        "Sé directo, riguroso, usa jerga entendible y limítate a un párrafo conciso.",
                composition.toString(), currentNotional, currentVolatility, currentVaR, currentCVaR
        );
    } // <-- Aquí cerramos correctamente getContextualData()

    // --- MÉTODOS DE RENDERIZADO DEL HEATMAP DE CORRELACIÓN ---

    private void renderizarHeatmapCorrelacion(double[][] matrix, List<String> assets) {
        gridCorrelation.removeAllViews();
        int n = assets.size();

        // Dimensiones: N+1 porque la primera fila y columna son las etiquetas (Tickers)
        gridCorrelation.setRowCount(n + 1);
        gridCorrelation.setColumnCount(n + 1);

        // Esquina superior izquierda (vacía)
        gridCorrelation.addView(crearCeldaHeatmap("", Color.TRANSPARENT, true));

        // Cabeceras de columnas (Tickers en el eje X)
        for (String asset : assets) {
            gridCorrelation.addView(crearCeldaHeatmap(asset, Color.parseColor("#333333"), true));
        }

        // Bucle para pintar filas y datos
        for (int i = 0; i < n; i++) {
            // Cabecera de fila (Ticker en el eje Y)
            gridCorrelation.addView(crearCeldaHeatmap(assets.get(i), Color.parseColor("#333333"), true));

            // Valores de la matriz rho_ij
            for (int j = 0; j < n; j++) {
                double val = matrix[i][j];
                int bgColor = calcularColorCorrelacion(val);
                String text = String.format(Locale.US, "%.2f", val);

                // Si es la diagonal principal (rho = 1), podemos ponerle un marcador visual o dejarlo normal
                gridCorrelation.addView(crearCeldaHeatmap(text, bgColor, false));
            }
        }
    }

    private int calcularColorCorrelacion(double rho) {
        // Interpola el color basado en la intensidad de rho en [-1, 1]
        int alpha = 255;
        int r = 0, g = 0, b = 0;

        if (rho > 0) {
            // Correlación positiva: de Negro (0) a Verde (255)
            g = (int) (rho * 200); // 200 en lugar de 255 para que no sea un verde cegador
        } else if (rho < 0) {
            // Correlación negativa: de Negro (0) a Rojo (255)
            r = (int) (Math.abs(rho) * 200);
        }

        return Color.argb(alpha, r, g, b);
    }

    private TextView crearCeldaHeatmap(String texto, int bgColor, boolean isHeader) {
        TextView tv = new TextView(getContext());
        tv.setText(texto);
        tv.setTextColor(Color.WHITE);
        tv.setBackgroundColor(bgColor);

        // El texto debe estar perfectamente centrado en los dos ejes
        tv.setGravity(android.view.Gravity.CENTER);

        // Ajustamos un poco el tamaño de fuente para que quepa bien en el cuadrado
        tv.setTextSize(isHeader ? 11f : 13f);

        if (isHeader) {
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        // --- LA MAGIA DE LA GEOMETRÍA FIJA ---
        // Forzamos cuadrados exactos de 50x50 dp (independientemente del texto)
        float factorDensidad = getResources().getDisplayMetrics().density;
        int tamanoCeldaPx = (int) (50 * factorDensidad);

        android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
        params.width = tamanoCeldaPx;  // Ancho rígido
        params.height = tamanoCeldaPx; // Alto rígido
        params.setMargins(2, 2, 2, 2); // Simula el borde de 2px

        tv.setLayoutParams(params);

        return tv;
    }
} // <-- Cierre final de la clase RiskFragment