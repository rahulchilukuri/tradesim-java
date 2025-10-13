package com.tradesim;

import java.util.List;
import java.util.Map;

public class Reporting {
    public static void printTradeSummary(Config cfg, List<Long> balanceHistory, List<Map<String, Object>> trades, double maxDrawdown) {
        long startingBalance = cfg.startingBalance;
        long finalBalance = balanceHistory.get(balanceHistory.size() - 1);
        long totalProfit = finalBalance - startingBalance;
        int tradesExecuted = balanceHistory.size() - 1;
        long wins = trades.stream().limit(tradesExecuted).filter(m -> "Win".equals(m.get("outcome"))).count();
        double winRate = tradesExecuted > 0 ? (wins / (double) tradesExecuted) * 100.0 : 0.0;

        System.out.println("-".repeat(80));
        System.out.println("\nSummary:");
        System.out.println("Trades Executed: " + tradesExecuted);
        System.out.println("Final Balance: $" + finalBalance);
        System.out.println("Total Profit: $" + totalProfit);
        System.out.printf("Maximum Drawdown: %.2f%%%n", maxDrawdown);
    }
}
