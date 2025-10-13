package com.tradesim;

import com.tradesim.model.Trade;

import java.util.*;

public class Simulator {
    public static Map<String, Object> simulateTrades(Config cfg, List<Trade> trades) {
        long balance = cfg.startingBalance;
        List<Long> balanceHistory = new ArrayList<>();
        balanceHistory.add(balance);
        long maxBalance = balance;
        double maxDrawdown = 0.0;

        double kellyFraction = Kelly.calculateKellyFraction(trades);
        double cappedKellyFraction = kellyFraction * cfg.kellyCap;
        Random rnd = new Random(cfg.seed);

        // sampled trades (with replacement)
        List<Trade> sampledTrades = new ArrayList<>();
        for (int i = 0; i < cfg.numTrades; i++) {
            sampledTrades.add(trades.get(rnd.nextInt(trades.size())));
        }

        List<Map<String, Object>> tradeLog = new ArrayList<>();
        for (int i = 0; i < sampledTrades.size(); i++) {
            Trade trade = sampledTrades.get(i);
            double currentRiskPercent = Risk.adjustRiskPercent(cappedKellyFraction, i, cfg.numTrades, cfg.riskDecayFactors);
            long riskAmount = (long) Math.floor(balance * currentRiskPercent);
            long start = balance;
            double actualRiskPct = start == 0 ? 0.0 : (riskAmount / (double) start) * 100.0;

            long reward = 0;
            String outcome;
            long amount;
            if (trade.win()) {
                long profit = (long) Math.floor(riskAmount * trade.rrr());
                long tax = cfg.calcTax ? (long) Math.floor(profit * (cfg.taxPct / 100.0)) : 0;
                reward = profit - tax;
                balance += reward;
                outcome = "Win";
                amount = reward;
            } else {
                balance -= riskAmount;
                reward = -riskAmount;
                outcome = "Loss";
                amount = -riskAmount;
            }

            if (balance < 0) balance = 0;
            balanceHistory.add(balance);
            if (balance > maxBalance) maxBalance = balance;
            double drawdown = maxBalance == 0 ? 0.0 : ((double)(maxBalance - balance) / maxBalance) * 100.0;
            if (drawdown > maxDrawdown) maxDrawdown = drawdown;

            Map<String, Object> entry = new HashMap<>();
            entry.put("trade_num", i + 1);
            entry.put("outcome", outcome);
            entry.put("rrr", trade.rrr());
            entry.put("actual_risk_pct", actualRiskPct);
            entry.put("risk_amount", riskAmount);
            entry.put("amount", amount);
            entry.put("start_balance", start);
            entry.put("end_balance", balance);
            tradeLog.add(entry);

            if (balance <= 0) {
                System.out.println("Account depleted after " + (i + 1) + " trades");
                break;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("balance_history", balanceHistory);
        result.put("sampled_trades", sampledTrades);
        result.put("max_drawdown", maxDrawdown);
        if(!cfg.montyCarlo)
            result.put("trade_log", tradeLog);
        return result;
    }
}
