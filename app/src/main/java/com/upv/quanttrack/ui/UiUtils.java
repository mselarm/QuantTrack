package com.upv.quanttrack.ui;

import android.os.Build;
import android.text.Html;
import android.text.Spanned;

public class UiUtils {

    /**
     * Toma el texto crudo de la capa de red (LLM) y lo renderiza para la vista.
     */
    public static Spanned formatLlmResponse(String rawMarkdown) {
        if (rawMarkdown == null || rawMarkdown.trim().isEmpty()) {
            return android.text.Spannable.Factory.getInstance().newSpannable("");
        }

        // 1. Limpieza de asteriscos dobles (Negrita)
        String processed = rawMarkdown.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");

        // 2. Limpieza de asteriscos simples (Cursiva)
        processed = processed.replaceAll("\\*(.*?)\\*", "<i>$1</i>");

        // 3. Respetamos los saltos de línea del LLM
        processed = processed.replaceAll("(\r\n|\n)", "<br>");

        // 4. Inyección al motor de renderizado de Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(processed, Html.FROM_HTML_MODE_LEGACY);
        } else {
            //noinspection deprecation
            return Html.fromHtml(processed);
        }
    }
}
