package com.upv.quanttrack.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.upv.quanttrack.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Carga el diseño del contenedor y la barra inferior que creamos antes
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Lógica de enrutamiento
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_micro) {
                selectedFragment = new MicroFragment(); // Carga tu gráfica actual
            } else if (itemId == R.id.nav_macro) {
                selectedFragment = new MacroFragment();
            } else if (itemId == R.id.nav_risk) {
                // Aquí irá el VaR más adelante
                selectedFragment = new Fragment();
            } else if (itemId == R.id.nav_options) {
                // Aquí irá Black-Scholes más adelante
                selectedFragment = new Fragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // 3. Forzar la carga de la Pestaña Micro por defecto al abrir la app
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_micro);
        }
    }
}