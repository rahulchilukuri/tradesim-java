package com.tradesim;

import com.tradesim.model.Trade;

import java.util.List;

public class Kelly {
    public static double calculateKellyFraction(List<Trade> trades) {
        if (trades == null || trades.isEmpty()) return 0.01;
        long wins = trades.stream().filter(Trade::win).count();
        double winRate = (double) wins / trades.size();
        double avgRrr = trades.stream().filter(Trade::win).mapToDouble(Trade::rrr).average().orElse(1.0);

        if (avgRrr <= 0 || winRate <= 0) return 0.01;
        double kelly = (winRate * (avgRrr + 1) - 1) / avgRrr;
        kelly = Math.max(0.0, Math.min(kelly, 1.0));
        return kelly;
    }
}
