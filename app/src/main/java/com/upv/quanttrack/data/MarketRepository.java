//package com.upv.quanttrack.data;
//
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//import android.util.Log;
//import java.util.Map;
//
//public class MarketRepository {
//
//    // Constante de la API Key
//    private static final String API_KEY = "LXBYH8RH3TQTZKOP";//"F8GRLBWMGTCIAJTA"; Solo hay 25 al dia despues de eso te capan
//
//    private AlphaVantageApi api;
//
//    // 1. Definimos el contrato de retorno (El puente hacia la Interfaz Gráfica)
//    public interface DataCallback {
//        void onSuccess(Map<String, DailyData> data);
//        void onError(String error);
//    }
//
//    public MarketRepository() {
//        // Inicializamos la interfaz usando el cliente Singleton
//        api = ApiClient.getClient().create(AlphaVantageApi.class);
//    }
//
//    // 2. Modificamos la firma: ahora recibe el ticker y el canal de retorno (callback)
//    public void fetchDailyData(String ticker, DataCallback callback) {
//
//        Call<AlphaVantageResponse> call = api.getDailyData(ticker, API_KEY);
//
//        call.enqueue(new Callback<AlphaVantageResponse>() {
//            @Override
//            public void onResponse(Call<AlphaVantageResponse> call, Response<AlphaVantageResponse> response) {
//                if (response.isSuccessful() && response.body() != null && response.body().getTimeSeries() != null) {
//
//                    // Éxito topológico: Devolvemos el mapa de datos completo al hilo principal (MainActivity)
//                    callback.onSuccess(response.body().getTimeSeries());
//
//                } else {
//                    // Si el servidor de AlphaVantage devuelve un límite de cuota o error
//                    callback.onError("Error HTTP del servidor: " + response.code());
//                }
//            }
//
//            @Override
//            public void onFailure(Call<AlphaVantageResponse> call, Throwable t) {
//                // Fallo físico (ej. modo avión activado)
//                callback.onError("Fallo físico de red: " + t.getMessage());
//            }
//        });
//    }
//}
package com.upv.quanttrack.data;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class MarketRepository {

    public interface DataCallback {
        void onSuccess(Map<String, DailyData> data);
        void onError(String error);
    }

    public MarketRepository() {
        // Inicialización vacía (hemos desconectado Retrofit temporalmente)
    }

    public void fetchDailyData(String ticker, DataCallback callback) {
        // 1. Construimos un JSON válido matemáticamente en memoria
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");

        Calendar cal = Calendar.getInstance();
        double precioBase = 150.0; // Precio inicial arbitrario

        // Generamos 200 días de datos para que la SMA de 200 pueda existir
        for (int i = 0; i < 200; i++) {
            String fecha = String.format(Locale.US, "%04d-%02d-%02d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH));

            // Variación aleatoria diaria
            double variacion = (Math.random() - 0.5) * 4.0;
            precioBase += variacion;

            // Formato exacto que espera tu clase DailyData
            String diaJson = String.format(Locale.US,
                    "\"%s\": {\"1. open\": \"%.2f\", \"2. high\": \"%.2f\", \"3. low\": \"%.2f\", \"4. close\": \"%.2f\", \"5. volume\": \"1000\"}",
                    fecha, precioBase - 1, precioBase + 2, precioBase - 2, precioBase);

            jsonBuilder.append(diaJson);

            if (i < 199) {
                jsonBuilder.append(",");
            }
            cal.add(Calendar.DAY_OF_YEAR, -1); // Retrocedemos un día
        }
        jsonBuilder.append("}");

        try {
            // 2. Usamos Gson para deserializar el String directamente a tu Map<String, DailyData>
            Type type = new TypeToken<Map<String, DailyData>>(){}.getType();
            Map<String, DailyData> mockData = new Gson().fromJson(jsonBuilder.toString(), type);

            // 3. Simulamos 0.5 segundos de latencia de red para no bloquear el UI Thread
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                callback.onSuccess(mockData);
            }, 500);

        } catch (Exception e) {
            callback.onError("Error en la inyección de datos simulados: " + e.getMessage());
        }
    }
}

