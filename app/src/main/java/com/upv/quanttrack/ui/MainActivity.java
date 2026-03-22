package com.upv.quanttrack.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.upv.quanttrack.R;
import com.upv.quanttrack.data.llm.LlmRepository;

public class MainActivity extends AppCompatActivity {

    // 1. Declaración a nivel de clase (fuera de los métodos)
    private LlmRepository llmRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 2. Instanciamos el motor de IA
        llmRepository = new LlmRepository();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Lógica de enrutamiento
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_micro) {
                selectedFragment = new MicroFragment();
            } else if (itemId == R.id.nav_macro) {
                selectedFragment = new MacroFragment();
            } else if (itemId == R.id.nav_risk) {
                selectedFragment = new RiskFragment();
            } else if (itemId == R.id.nav_options) {
                selectedFragment = new VolatilityFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Forzar la carga de la Pestaña Micro por defecto
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_micro);
        }

        // 3. El interceptor del Botón Flotante (FAB) - ACCESO UNIVERSAL AL GLOSARIO
        FloatingActionButton fabGemini = findViewById(R.id.fabGemini);
        fabGemini.setOnClickListener(v -> {
            // Buscamos qué fragmento está metido en el contenedor ahora mismo
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

            if (currentFragment instanceof Analyzable) {
                Analyzable analyzableTab = (Analyzable) currentFragment;
                String prompt = analyzableTab.getContextualData();
                String tabName = analyzableTab.getTabName();

                // --- PASO 1: Abrimos la biblioteca SIEMPRE ---
                GlossaryBottomSheet bottomSheet = new GlossaryBottomSheet();

                // --- PASO 2: Configuramos el disparo de la IA con el cortafuegos interno ---
                bottomSheet.setAiTriggerListener(() -> {

                    // Verificamos si los tensores de datos están vacíos
                    boolean datosInvalidos = prompt.contains("Aún no hay") ||
                            prompt.contains("La cartera no") ||
                            prompt.contains("No hay ningún");

                    if (datosInvalidos) {
                        // Si no hay datos, mostramos el aviso pero NO llamamos a mostrarDialogoIA
                        Toast.makeText(this, "Para un análisis personalizado: " + prompt, Toast.LENGTH_LONG).show();
                    } else {
                        // Si hay datos, procedemos con Gemini
                        mostrarDialogoIA(tabName, prompt);
                    }
                });

                bottomSheet.show(getSupportFragmentManager(), "GlossarySheet");

            } else {
                Toast.makeText(this, "Esta pestaña aún no soporta análisis por IA.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 4. El motor visual del Popup
    private void mostrarDialogoIA(String tabName, String prompt) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Gemini Pro - " + tabName);
        builder.setMessage("Analizando tensores matemáticos...\nPor favor, espera.");
        builder.setCancelable(false);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show(); // Dibujamos el popup en pantalla

        // --- LA SOLUCIÓN DEL SCROLL ---
        // Extraemos el cuadro de texto nativo del Dialog que acabamos de mostrar
        android.widget.TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            // Le inyectamos el motor de desplazamiento vertical nativo
            messageView.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        }

        // Disparamos la petición a la red
        llmRepository.analyzeMarket(prompt, new com.upv.quanttrack.data.llm.LlmRepository.LlmCallback() {
            @Override
            public void onSuccess(String analysis) {
                runOnUiThread(() -> {
                    // Simplemente inyectamos el nuevo texto formateado.
                    // Como el motor de scroll ya está activo, si el texto es muy largo, se podrá deslizar.
                    dialog.setMessage(UiUtils.formatLlmResponse(analysis));
                    dialog.setCancelable(true);
                    dialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE, "Entendido", (d, which) -> d.dismiss());
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    dialog.setMessage("Fallo de conexión: " + error);
                    dialog.setCancelable(true);
                    dialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE, "Cerrar", (d, which) -> d.dismiss());
                });
            }
        });
    }
}