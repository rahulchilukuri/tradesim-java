package com.tradesim;

import com.tradesim.model.Trade;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        Config cfg = Config.montyConfig();
        //Config cfg = Config.fromJsonFile("config.json");

        long start = System.currentTimeMillis();
        if(cfg.montyCarlo) {
            MonteCarlo.runMonteCarloParallel(cfg);
        } else {
            MonteCarlo.runAndSummarizeSingleSimulation(cfg);
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("Simulation completed in %.2f seconds.%n", elapsed / 1000.0);
    }
}
