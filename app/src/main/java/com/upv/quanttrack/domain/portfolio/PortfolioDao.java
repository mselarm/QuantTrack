package com.upv.quanttrack.domain.portfolio;



import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PortfolioDao {

    // Estrategia REPLACE: Si insertas AAPL y ya existe, actualiza sus acciones (Upsert)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAsset(PortfolioAsset asset);

    @Query("DELETE FROM portfolio_table WHERE ticker = :ticker")
    void deleteAsset(String ticker);

    // Truncate de la tabla para el botón CLR
    @Query("DELETE FROM portfolio_table")
    void clearPortfolio();

    // Extracción secuencial del vector de estado al arrancar la app
    @Query("SELECT * FROM portfolio_table")
    List<PortfolioAsset> getAllAssets();
}
