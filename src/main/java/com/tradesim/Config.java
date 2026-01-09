package com.tradesim;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.Arrays;

public class Config {
    static final ObjectMapper mapper = new ObjectMapper();
    public long startingBalance = 10_000;
    public int numTrades = 250;
    public int reportInterval = 25;
    public long seed = 23;
    public boolean calcTax = true;
    public double taxPct = 30.0;
    public double maxRiskPrct = 0.01;
    public double kellyCap = 0.1;
    public boolean reportTrades = true;
    public double minRr = 3;
    public double maxRr = 20;
    public int numSampleTrades = 100;
    public double winRateLow = 0.28;
    public double winRateHigh = 0.31;
    public double[] riskDecayFactors = new double[] { 1.0, 0.9, 0.8, 0.7, 0.6 };

    // Monte Carlo specific
    public boolean montyCarlo = false;
    public int batchSize = 10_000;
    public long targetBalance = 50_000_000;
    public int numSimulations = 1000;
    public int numMonteCarloWorkers = 100;
    public boolean printTradeSamples = false;

    public Config copyWithSeed(long newSeed) {
        Config c = new Config();
        c.startingBalance = this.startingBalance;
        c.numTrades = this.numTrades;
        c.reportInterval = this.reportInterval;
        c.seed = newSeed;
        c.calcTax = this.calcTax;
        c.taxPct = this.taxPct;
        c.maxRiskPrct = this.maxRiskPrct;
        c.kellyCap = this.kellyCap;
        c.reportTrades = this.reportTrades;
        c.minRr = this.minRr;
        c.maxRr = this.maxRr;
        c.numSampleTrades = this.numSampleTrades;
        c.winRateLow = this.winRateLow;
        c.winRateHigh = this.winRateHigh;
        c.riskDecayFactors = Arrays.copyOf(this.riskDecayFactors, this.riskDecayFactors.length);
        c.montyCarlo = this.montyCarlo;
        c.batchSize = this.batchSize;
        c.targetBalance = this.targetBalance;
        c.numSimulations = this.numSimulations;
        c.numMonteCarloWorkers = this.numMonteCarloWorkers;
        c.printTradeSamples = this.printTradeSamples;
        return c;
    }

    public static Config singleRunConfig() throws Exception {
        Config c = mapper.readValue(Config.class.getResourceAsStream("/config.json"), Config.class);
        c.montyCarlo = false;
        return c;
    }

    public static Config montyConfig() throws Exception {
        // Config c = new Config();
        // c.montyCarlo = true;
        // c.batchSize = 100_000;
        // c.targetBalance = 20_000_000;
        // c.numSimulations = 1_000_000;
        // c.numMonteCarloWorkers = 100_000;
        // return c;
        Config c = mapper.readValue(Config.class.getResourceAsStream("/config.json"), Config.class);
        return c;
    }
}
