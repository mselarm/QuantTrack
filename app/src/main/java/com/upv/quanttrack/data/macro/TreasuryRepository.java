package com.upv.quanttrack.data.macro;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TreasuryRepository {

    private TreasuryApi api;

    public interface YieldCallback {
        void onSuccess(List<YieldCurveData> historicalData);
        void onError(String error);
    }

    public TreasuryRepository() {
        api = TreasuryClient.getClient().create(TreasuryApi.class);
    }

    public void fetchYieldCurveMatrix(YieldCallback callback) {
        Call<ResponseBody> call = api.getYieldCurveCsv();

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // 1. Extraemos el CSV entero como un gran String
                        String csvData = response.body().string();
                        String[] lines = csvData.split("\n");
                        List<YieldCurveData> parsedData = new ArrayList<>();

                        // 2. El CSV tiene cabecera en la línea 0 y está ordenado cronológicamente.
                        // Iteramos de abajo hacia arriba para que el índice 0 sea el día de hoy.
                        for (int i = 1; i < lines.length; i++) {
                            String line = lines[i].trim();
                            if (line.isEmpty()) continue;

                            String[] columns = line.split(",");
                            if (columns.length < 14) continue; // Defensa contra líneas incompletas

                            YieldCurveData day = new YieldCurveData();

                            // 3. Mapeo matricial exacto de las columnas del Tesoro
                            day.date = columns[0].replace("\"", "");
                            day.m1 = columns[1].replace("\"", "");
                            day.m3 = columns[3].replace("\"", ""); // Saltamos el 2M (col 2)
                            day.m6 = columns[5].replace("\"", ""); // Saltamos el 4M (col 4)
                            day.y1 = columns[6].replace("\"", "");
                            day.y2 = columns[7].replace("\"", "");
                            day.y3 = columns[8].replace("\"", "");
                            day.y5 = columns[9].replace("\"", "");
                            day.y7 = columns[10].replace("\"", "");
                            day.y10 = columns[11].replace("\"", "");
                            day.y20 = columns[12].replace("\"", "");
                            day.y30 = columns[13].replace("\"", "");

                            parsedData.add(day);
                        }

                        callback.onSuccess(parsedData);

                    } catch (IOException e) {
                        callback.onError("Error leyendo flujo CSV: " + e.getMessage());
                    }
                } else {
                    callback.onError("Error HTTP bloqueado: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onError("Fallo físico de red: " + t.getMessage());
            }
        });
    }
}
