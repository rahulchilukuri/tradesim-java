package com.tradesim;

public class Main {
    public static void main(String[] args) throws Exception {
        Config cfg;
        if(System.getProperty("mode") == null) {
            cfg = Config.singleRunConfig();
        } else {
            cfg = Config.montyConfig();
        }

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
