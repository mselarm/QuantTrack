package com.upv.quanttrack.data;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class MarketRepository {

    private final YahooFinanceApi api;

    public MarketRepository() {
        // 1. Interceptor de Camuflaje (Bypass antibot de Yahoo)
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                })
                .build();

        // 2. Cliente HTTP inyectado
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://query1.finance.yahoo.com/")
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        api = retrofit.create(YahooFinanceApi.class);
    }

    public interface DataCallback {
        void onSuccess(Map<String, DailyData> data);
        void onError(String error);
    }

    public void fetchDailyData(String ticker, DataCallback callback) {
        // Atacamos el endpoint v8 de gráficas pidiendo velas diarias ("1d") de los últimos 2 años ("2y")
        api.getHistoricalData(ticker, "1d", "2y").enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                // Filtro 1: Código HTTP 404 (Not Found) a nivel de red
                if (response.code() == 404) {
                    callback.onError("Ticker no encontrado");
                    return;
                }

                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("HTTP " + response.code() + ": Error del servidor");
                    return;
                }

                try {
                    org.json.JSONObject root = new org.json.JSONObject(response.body());
                    org.json.JSONObject chart = root.getJSONObject("chart");

                    // Filtro 2: Yahoo devuelve HTTP 200 OK pero inyecta un nodo "error" en el JSON
                    if (!chart.isNull("error")) {
                        callback.onError("Ticker no encontrado");
                        return;
                    }

                    // --- EXTRACCIÓN DE DATOS ---
                    org.json.JSONObject result = chart.getJSONArray("result").getJSONObject(0);
                    org.json.JSONArray timestamps = result.getJSONArray("timestamp");

                    org.json.JSONObject quote = result.getJSONObject("indicators")
                            .getJSONArray("quote").getJSONObject(0);

                    org.json.JSONArray openArr = quote.getJSONArray("open");
                    org.json.JSONArray highArr = quote.getJSONArray("high");
                    org.json.JSONArray lowArr = quote.getJSONArray("low");
                    org.json.JSONArray closeArr = quote.getJSONArray("close");
                    // 1. Extraemos el array de volumen del JSON
                    org.json.JSONArray volumeArr = quote.getJSONArray("volume");

                    Map<String, DailyData> marketData = new HashMap<>();

                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

                    for (int i = 0; i < timestamps.length(); i++) {
                        // 2. Exigimos que ni el cierre ni el volumen sean nulos (evita datos corruptos)
                        if (!closeArr.isNull(i) && !volumeArr.isNull(i)) {
                            long ts = timestamps.getLong(i) * 1000L;
                            String date = sdf.format(new java.util.Date(ts));

                            double open = openArr.getDouble(i);
                            double high = highArr.getDouble(i);
                            double low = lowArr.getDouble(i);
                            double close = closeArr.getDouble(i);
                            // 3. Extraemos el valor escalar iterado
                            double volume = volumeArr.getDouble(i);

                            // 4. Inyectamos el volumen en el constructor de memoria
                            marketData.put(date, new DailyData(open, high, low, close, volume));
                        }
                    }

                    callback.onSuccess(marketData);

                } catch (Exception e) {
                    callback.onError("Fallo de parseo matricial: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                callback.onError("Fallo físico de red (Timeout): " + t.getMessage());
            }
        });
    }
}