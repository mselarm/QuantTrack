package com.upv.quanttrack.ui;

public interface Analyzable {
    // Devuelve el nombre de la pestaña actual
    String getTabName();

    // Devuelve el prompt técnico crudo listo para Gemini
    String getContextualData();
}
