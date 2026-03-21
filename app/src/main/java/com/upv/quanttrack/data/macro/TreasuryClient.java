package com.upv.quanttrack.data.macro;

import retrofit2.Retrofit;

public class TreasuryClient {
    // Apuntamos al frontend público, no a la API
    private static final String BASE_URL = "https://home.treasury.gov/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    // Eliminamos el GsonConverterFactory. Vamos a leer el String crudo.
                    .build();
        }
        return retrofit;
    }
}