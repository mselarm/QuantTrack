package com.upv.quanttrack.data.macro;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

public class TreasuryClient {
    // Apuntamos al frontend público, no a la API
    private static final String BASE_URL = "https://home.treasury.gov/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {

            // 1. Construimos el cliente tolerante a la latencia del gobierno
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS) // 30s para establecer conexión
                    .readTimeout(30, TimeUnit.SECONDS)    // 30s para descargar el CSV
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            // 2. Ensamblamos Retrofit inyectando nuestro cliente
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient) // <--- Aquí blindamos la conexión
                    // Eliminamos el GsonConverterFactory. Vamos a leer el String crudo.
                    .build();
        }
        return retrofit;
    }
}