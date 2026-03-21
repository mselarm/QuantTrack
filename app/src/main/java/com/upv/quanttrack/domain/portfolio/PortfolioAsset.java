package com.upv.quanttrack.domain.portfolio;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "portfolio_table")
public class PortfolioAsset {

    // El ticker actúa como clave primaria para evitar activos duplicados
    @PrimaryKey
    @NonNull
    private String ticker;

    private double shares;

    public PortfolioAsset(@NonNull String ticker, double shares) {
        this.ticker = ticker;
        this.shares = shares;
    }

    @NonNull
    public String getTicker() { return ticker; }
    public double getShares() { return shares; }
}
