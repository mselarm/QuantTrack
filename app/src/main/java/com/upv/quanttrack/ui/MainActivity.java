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

        // 3. El interceptor del Botón Flotante (FAB)
        FloatingActionButton fabGemini = findViewById(R.id.fabGemini);
        fabGemini.setOnClickListener(v -> {
            // Buscamos exactamente qué fragmento está metido en el contenedor ahora mismo
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

            // Verificamos si ese fragmento ha firmado nuestro contrato "Analyzable"
            if (currentFragment instanceof Analyzable) {
                Analyzable analyzableTab = (Analyzable) currentFragment;
                String prompt = analyzableTab.getContextualData();
                String tabName = analyzableTab.getTabName();

                // Mostramos el popup y llamamos a la API
                mostrarDialogoIA(tabName, prompt);
            } else {
                Toast.makeText(this, "Esta pestaña aún no soporta análisis por IA.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 4. El motor visual del Popup
    private void mostrarDialogoIA(String tabName, String prompt) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Gemini Pro - " + tabName);
        builder.setMessage("Analizando tensores matemáticos...\nPor favor, espera.");
        builder.setCancelable(false); // Bloquea toques fuera de la ventana

        AlertDialog dialog = builder.create();
        dialog.show();

        // Disparamos la petición a la red
        llmRepository.analyzeMarket(prompt, new LlmRepository.LlmCallback() {
            @Override
            public void onSuccess(String analysis) {
                runOnUiThread(() -> {
                    dialog.setMessage(analysis);
                    dialog.setCancelable(true);
                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Entendido", (d, which) -> d.dismiss());
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    dialog.setMessage("Fallo de conexión: " + error);
                    dialog.setCancelable(true);
                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Cerrar", (d, which) -> d.dismiss());
                });
            }
        });
    }
}