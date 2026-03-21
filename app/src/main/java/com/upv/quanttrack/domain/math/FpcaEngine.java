package com.upv.quanttrack.domain.math;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.Covariance;

import java.util.Arrays;

public class FpcaEngine {

    private RealMatrix eigenVectors;
    private double[] eigenValues;
    private double[] meanVector;

    /**
     * @param rawData Matriz N x 11 (N días, 11 vencimientos).
     * Debe estar cronológicamente ordenada (ej. fila 0 es t=0, fila N es t=hoy).
     */
    public FpcaResult decomposeYieldCurve(double[][] rawData) {
        int numDays = rawData.length;
        int numMaturities = rawData[0].length; // Debería ser 11

        // 1. Centrado de la Media (Amplitude Translation)
        meanVector = new double[numMaturities];
        for (int j = 0; j < numMaturities; j++) {
            double sum = 0;
            for (int i = 0; i < numDays; i++) {
                sum += rawData[i][j];
            }
            meanVector[j] = sum / numDays;
        }

        double[][] centeredData = new double[numDays][numMaturities];
        for (int i = 0; i < numDays; i++) {
            for (int j = 0; j < numMaturities; j++) {
                centeredData[i][j] = rawData[i][j] - meanVector[j];
            }
        }

        // 2. Matriz de Covarianza Empírica Sigma
        RealMatrix centeredMatrix = new Array2DRowRealMatrix(centeredData);
        Covariance covariance = new Covariance(centeredMatrix);
        RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();

        // 3. Descomposición en Valores Singulares (EigenDecomposition)
        EigenDecomposition ed = new EigenDecomposition(covarianceMatrix);

        // Apache ordena los autovalores de mayor a menor por defecto
        eigenValues = ed.getRealEigenvalues();
        eigenVectors = ed.getV(); // Matriz donde cada COLUMNA es un autovector

        // 4. Proyección de Scores (Z = X_c * V)
        // Obtenemos la evolución temporal de los 3 componentes principales
        RealMatrix scores = centeredMatrix.multiply(eigenVectors);

        double[] pc1Scores = scores.getColumn(0); // Nivel
        double[] pc2Scores = scores.getColumn(1); // Pendiente
        double[] pc3Scores = scores.getColumn(2); // Curvatura

        // Cálculo de Varianza Explicada
        double totalVariance = Arrays.stream(eigenValues).sum();
        double varPc1 = (eigenValues[0] / totalVariance) * 100;
        double varPc2 = (eigenValues[1] / totalVariance) * 100;
        double varPc3 = (eigenValues[2] / totalVariance) * 100;

        return new FpcaResult(pc1Scores, pc2Scores, pc3Scores, varPc1, varPc2, varPc3, meanVector, eigenVectors.getData());
    }

    // Clase DTO estática para transportar los tensores de salida
    public static class FpcaResult {
        public final double[] pc1Scores;
        public final double[] pc2Scores;
        public final double[] pc3Scores;
        public final double varPc1, varPc2, varPc3;
        public final double[] meanVector;
        public final double[][] eigenVectors;

        public FpcaResult(double[] pc1, double[] pc2, double[] pc3,
                          double var1, double var2, double var3,
                          double[] mean, double[][] eigenVecs) {
            this.pc1Scores = pc1;
            this.pc2Scores = pc2;
            this.pc3Scores = pc3;
            this.varPc1 = var1;
            this.varPc2 = var2;
            this.varPc3 = var3;
            this.meanVector = mean;
            this.eigenVectors = eigenVecs;
        }
    }
}