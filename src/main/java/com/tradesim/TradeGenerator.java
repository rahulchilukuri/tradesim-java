package com.tradesim;

import com.tradesim.model.Trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TradeGenerator {
    public static List<Trade> generateSampleTrades(Config cfg) {
        Random rnd = new Random(cfg.seed);
        int numSamples = cfg.numSampleTrades;
        int low = (int) Math.floor(numSamples * cfg.winRateLow);
        int high = (int) Math.floor(numSamples * cfg.winRateHigh);
        int targetWins = low >= high ? low : low + rnd.nextInt(Math.max(1, high - low));

        List<Trade> trades = new ArrayList<>(numSamples);
        for (int i = 0; i < numSamples; i++) {
            boolean win = i < targetWins;
            double rrr;
            if (win) {
                double meanRrr = (cfg.minRr + cfg.maxRr) / 2.0;
                double sigma = 0.5;
                double mu = Math.log(meanRrr) - 0.5 * sigma * sigma;
                rrr = Math.exp(mu + sigma * rnd.nextGaussian());
                rrr = Math.max(cfg.minRr, Math.min(cfg.maxRr, rrr));
            } else {
                rrr = cfg.minRr == 0 ? 1.0 : 1.0 + rnd.nextDouble() * 0.25;
            }
            trades.add(new Trade(Math.round(rrr * 100.0) / 100.0, win));
        }
        Collections.shuffle(trades, rnd);
        return trades;
    }
}
