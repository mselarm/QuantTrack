package com.upv.quanttrack.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.upv.quanttrack.R;
import com.upv.quanttrack.data.QuantTerm;

import java.util.ArrayList;
import java.util.List;

public class GlossaryBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView rvGlossary;
    private EditText etSearchGlossary;
    private Button btnTriggerAi;
    private GlossaryAdapter adapter;

    // Interfaz para avisar al MainActivity de que el usuario quiere usar la IA
    public interface AiTriggerListener {
        void onTriggerAi();
    }

    private AiTriggerListener listener;

    public void setAiTriggerListener(AiTriggerListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_glossary_sheet, container, false);

        rvGlossary = view.findViewById(R.id.rvGlossary);
        etSearchGlossary = view.findViewById(R.id.etSearchGlossary);
        btnTriggerAi = view.findViewById(R.id.btnTriggerAi);

        configurarBuscadorYLista();

        btnTriggerAi.setOnClickListener(v -> {
            dismiss(); // Cerramos el panel inferior
            if (listener != null) listener.onTriggerAi(); // Disparamos la IA
        });

        return view;
    }

    private void configurarBuscadorYLista() {
        rvGlossary.setLayoutManager(new LinearLayoutManager(getContext()));

        // Aquí inyectamos el conocimiento matemático de tu aplicación
        List<QuantTerm> terms = new ArrayList<>();
        terms.add(new QuantTerm("Value at Risk (VaR)",
                "Medida estadística de riesgo de mercado que cuantifica la pérdida máxima potencial de una cartera en un horizonte temporal (ej. 1 día) con un nivel de confianza (ej. 95%).\n\n" +
                        "Interpretación: Si el VaR(95%) es $100, existe un 5% de probabilidad de que la pérdida mañana sea superior a $100. Su limitación principal es que 'ignora' la magnitud de las pérdidas extremas en el percentil crítico, asumiendo una distribución normal de retornos (no contempla 'cisnes negros')."));

        terms.add(new QuantTerm("Expected Shortfall (CVaR)",
                "Media ponderada de las pérdidas que exceden el umbral del VaR. Se conoce como 'VaR Condicional' y es una métrica de riesgo coherente que mide la severidad del colapso en la cola de la distribución.\n\n" +
                        "Interpretación: Mientras el VaR te dice 'perderás al menos X', el CVaR responde a '¿cuánto perderé de media si las cosas salen realmente mal?'. Es fundamental para detectar el Riesgo de Cola (Fat Tails); si el CVaR es mucho mayor que el VaR, la cartera es vulnerable a eventos sistémicos violentos."));
        terms.add(new QuantTerm("Beta (β) - Sensibilidad Sistémica",
                "Coeficiente de regresión lineal que mide la volatilidad de un activo respecto a un índice de referencia (S&P 500). Es el ratio entre la covarianza (Activo/Mercado) y la varianza del Mercado.\n\n" +
                        "Interpretación: \n" +
                        "• β > 1.0: El activo es un 'amplificador'. Si el mercado sube un 1%, el activo tiende a subir un β%. Es óptimo para capturar momentum alcista pero peligroso en correcciones.\n" +
                        "• β < 1.0: Activo defensivo/inelástico. Protege el capital en mercados bajistas.\n" +
                        "• β ≈ 0: Activo descorrelacionado. Útil para diversificación pura."));

        terms.add(new QuantTerm("Eigenvectores y FPCA (Curva de Tipos)",
                "Técnica de reducción de dimensionalidad que descompone los movimientos de la estructura temporal de tipos de interés en tres factores ortogonales (PC1, PC2, PC3) que explican >95% de la varianza.\n\n" +
                        "Análisis de Resultados:\n" +
                        "• PC1 (Nivel/Level): Desplazamiento paralelo de toda la curva. Un PC1 positivo indica que todos los tipos (corto y largo plazo) suben a la vez.\n" +
                        "• PC2 (Pendiente/Slope): Movimiento de 'pivote'. Un PC2 alto indica que el diferencial entre tipos largos y cortos se ensancha (steepening) o se estrecha (flattening).\n" +
                        "• PC3 (Curvatura/Curvature): Movimiento de 'joroba'. Indica si los tipos de plazo intermedio suben o bajan respecto a los extremos, señalando cambios en las expectativas de inflación o política monetaria."));
        terms.add(new QuantTerm("Volumen Relativo (RVOL)",
                "Ratio que normaliza el volumen de negociación actual dividiéndolo por su media móvil histórica (usualmente de 20 o 50 días) para el mismo intervalo temporal.\n\n" +
                        "Análisis de Flujos:\n" +
                        "• RVOL > 2.0: Indica una anomalía de liquidez. Es la huella digital de la participación institucional (Smart Money). Los movimientos de precio con RVOL alto tienen una validez estadística superior, ya que confirman una ruptura de equilibrio entre oferta y demanda.\n" +
                        "• RVOL < 1.0: Falta de interés o 'secado' del mercado. Las rupturas de niveles con volumen bajo suelen ser trampas (bull/bear traps) por falta de convicción."));

        terms.add(new QuantTerm("Average True Range (ATR)",
                "Indicador de volatilidad que mide el rango medio de movimiento de un activo. A diferencia del rango simple, el ATR incluye los 'Gaps' (huecos) de apertura al calcular el 'True Range' como el máximo de: (High-Low), |High-Close_prev|, o |Low-Close_prev|.\n\n" +
                        "Aplicación Cuantitativa:\n" +
                        "• Gestión de Riesgo: Se utiliza para establecer Stop Loss dinámicos (ej. 2 x ATR). Si el ATR es $5, un stop a $10 protege la posición del ruido estadístico del mercado.\n" +
                        "• Ajuste de Posición: Activos con ATR alto requieren un tamaño de posición (lotaje) menor para mantener el riesgo monetario constante en la cartera."));
        terms.add(new QuantTerm("Media Móvil (SMA)",
                "Filtro de paso bajo que promedia 'n' cierres para extraer la tendencia.\n\n" +
                        "Interpretación: Actúa como soporte o resistencia dinámica. Si el precio cruza al alza, el régimen cambia a alcista. Las medias de largo plazo (200d) definen la estructura del mercado, mientras que las cortas (20d) definen el momento."));

        terms.add(new QuantTerm("Fuerza Relativa (RSI)",
                "Oscilador de momento [0-100] que mide la velocidad del precio.\n\n" +
                        "Interpretación: >70 es Sobrecompra (agotamiento alcista); <30 es Sobreventa (pánico técnico). La señal más potente ocurre en las 'Divergencias': cuando el precio sube pero el RSI baja, el movimiento carece de fuerza real y el giro es inminente."));
        terms.add(new QuantTerm("Tasa de Interés (Risk-Free Rate)",
                "Es el coste de oportunidad del capital y el eje sobre el cual se descuentan todos los activos financieros. En modelos cuantitativos, se utiliza el rendimiento del bono soberano a 10 años (Treasury Yield) como la tasa libre de riesgo ($R_f$).\n\n" +
                        "Mecánica y Valuación:\n" +
                        "• Descuento de Flujos: Una subida en las tasas aumenta el denominador en los modelos de valoración (WACC), lo que reduce automáticamente el Valor Presente de los beneficios futuros. Esto castiga especialmente a las acciones de 'Crecimiento' (Growth).\n" +
                        "• Curva de Tipos: La relación entre tasas de corto y largo plazo indica el ciclo económico. Una curva invertida (tasas cortas > largas) ha precedido históricamente a casi todas las recesiones modernas."));

        terms.add(new QuantTerm("Volatilidad (σ - Desviación Estándar)",
                "Medida de la dispersión de los retornos de un activo respecto a su media aritmética en un periodo determinado. Representa la incertidumbre o el 'riesgo total' (sistemático + específico) del instrumento.\n\n" +
                        "Análisis Cuantitativo:\n" +
                        "• Volatilidad Realizada: Cálculo histórico basado en la desviación estándar de los cierres logarítmicos. Indica cuánto se ha movido el activo en el pasado.\n" +
                        "• Volatilidad Implícita (IV): Extraída de los precios de las opciones (Modelo Black-Scholes). Refleja la expectativa del mercado sobre el movimiento futuro. Una IV alta encarece las primas de las opciones y sugiere un régimen de 'miedo' o incertidumbre inminente."));
        adapter = new GlossaryAdapter(terms);
        rvGlossary.setAdapter(adapter);

        // Lógica del filtro en tiempo real
        etSearchGlossary.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // --- ADAPTADOR INTERNO ---
    private static class GlossaryAdapter extends RecyclerView.Adapter<GlossaryAdapter.ViewHolder> implements Filterable {
        private final List<QuantTerm> termsFull;
        private List<QuantTerm> termsFiltered;

        public GlossaryAdapter(List<QuantTerm> terms) {
            this.termsFull = new ArrayList<>(terms);
            this.termsFiltered = new ArrayList<>(terms);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_glossary, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            QuantTerm term = termsFiltered.get(position);
            holder.tvTermName.setText(term.getTerm());
            holder.tvTermDefinition.setText(term.getDefinition());
        }

        @Override
        public int getItemCount() {
            return termsFiltered.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    List<QuantTerm> filteredList = new ArrayList<>();
                    if (constraint == null || constraint.length() == 0) {
                        filteredList.addAll(termsFull);
                    } else {
                        String filterPattern = constraint.toString().toLowerCase().trim();
                        for (QuantTerm item : termsFull) {
                            if (item.getTerm().toLowerCase().contains(filterPattern) ||
                                    item.getDefinition().toLowerCase().contains(filterPattern)) {
                                filteredList.add(item);
                            }
                        }
                    }
                    FilterResults results = new FilterResults();
                    results.values = filteredList;
                    return results;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    termsFiltered.clear();
                    termsFiltered.addAll((List<QuantTerm>) results.values);
                    notifyDataSetChanged();
                }
            };
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTermName, tvTermDefinition;
            ViewHolder(View itemView) {
                super(itemView);
                tvTermName = itemView.findViewById(R.id.tvTermName);
                tvTermDefinition = itemView.findViewById(R.id.tvTermDefinition);
            }
        }
    }
}
