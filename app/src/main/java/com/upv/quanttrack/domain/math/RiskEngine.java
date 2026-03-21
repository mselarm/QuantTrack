package com.upv.quanttrack.domain.math;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.Covariance;

import java.util.Arrays;

public class RiskEngine {

    private static final double Z_SCORE_95 = 1.645;
    private static final int TRADING_DAYS_YEAR = 252;

    public RiskResult calculatePortfolioRisk(double[][] returnsMatrix, double[] weights, double notional) {

        int nDays = returnsMatrix.length;
        int mAssets = weights.length;

        // 1. Matriz de Covarianza Empírica (Sigma)
        RealMatrix returnsRealMatrix = new Array2DRowRealMatrix(returnsMatrix);
        Covariance covariance = new Covariance(returnsRealMatrix);
        RealMatrix covMatrix = covariance.getCovarianceMatrix();

        // 2. Varianza de la Cartera (Paramétrica)
        RealMatrix w = new Array2DRowRealMatrix(weights);
        RealMatrix wT = w.transpose();

        RealMatrix portfolioVarianceMatrix = wT.multiply(covMatrix).multiply(w);
        double dailyVolatility = Math.sqrt(portfolioVarianceMatrix.getEntry(0, 0));
        double annualizedVolatility = dailyVolatility * Math.sqrt(TRADING_DAYS_YEAR);
        double var95 = notional * Z_SCORE_95 * dailyVolatility;

        // 3. Matriz de Correlación (Normalización de la Covarianza)
        // rho_ij = cov_ij / (sigma_i * sigma_j)
        double[][] corrMatrix = new double[mAssets][mAssets];
        for (int i = 0; i < mAssets; i++) {
            for (int j = 0; j < mAssets; j++) {
                corrMatrix[i][j] = covMatrix.getEntry(i, j) /
                        (Math.sqrt(covMatrix.getEntry(i, i)) * Math.sqrt(covMatrix.getEntry(j, j)));
            }
        }

        // 4. Expected Shortfall (CVaR Empírico al 95%)
        // Proyectamos los retornos históricos sobre los pesos actuales
        double[] portfolioReturns = new double[nDays];
        for (int t = 0; t < nDays; t++) {
            double dailyRet = 0.0;
            for (int j = 0; j < mAssets; j++) {
                dailyRet += weights[j] * returnsMatrix[t][j];
            }
            portfolioReturns[t] = dailyRet;
        }

        // Ordenamos de peor a mejor retorno (cola izquierda primero)
        Arrays.sort(portfolioReturns);

        // Calculamos la esperanza del 5% de los peores escenarios (cola gorda)
        int tailIndex = (int) Math.ceil(nDays * 0.05);
        double sumTail = 0.0;
        for (int t = 0; t < tailIndex; t++) {
            sumTail += portfolioReturns[t];
        }

        // Evitamos división por cero si la muestra es muy pequeña
        double expectedShortfallReturn = tailIndex > 0 ? (sumTail / tailIndex) : 0.0;
        double cVar95 = Math.abs(expectedShortfallReturn) * notional;

        return new RiskResult(annualizedVolatility, var95, cVar95, covMatrix.getData(), corrMatrix);
    }

    public static class RiskResult {
        public final double annualizedVolatility;
        public final double var95;
        public final double cVar95;
        public final double[][] covarianceMatrix;
        public final double[][] correlationMatrix;

        public RiskResult(double annualizedVolatility, double var95, double cVar95,
                          double[][] covarianceMatrix, double[][] correlationMatrix) {
            this.annualizedVolatility = annualizedVolatility;
            this.var95 = var95;
            this.cVar95 = cVar95;
            this.covarianceMatrix = covarianceMatrix;
            this.correlationMatrix = correlationMatrix;
        }
    }
}
