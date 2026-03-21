package com.upv.quanttrack.domain.portfolio;


import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// Versión 1 del esquema. Si en el futuro añades más variables (ej. precio de compra), subirás la versión.
@Database(entities = {PortfolioAsset.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract PortfolioDao portfolioDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "quant_track_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
