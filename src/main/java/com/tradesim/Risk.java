package com.tradesim;

public class Risk {
    public static double adjustRiskPercent(double baseRisk, int tradeIndex, int totalTrades, double[] decayFactors) {
        int quarter = Math.max(1, totalTrades / 5);
        double factor;
        if (tradeIndex >= 4 * quarter) factor = decayFactors[4];
        else if (tradeIndex >= 3 * quarter) factor = decayFactors[3];
        else if (tradeIndex >= 2 * quarter) factor = decayFactors[2];
        else if (tradeIndex >= quarter) factor = decayFactors[1];
        else factor = decayFactors[0];
        return baseRisk * factor;
    }
}
