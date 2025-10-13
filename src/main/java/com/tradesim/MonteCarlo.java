package com.tradesim;

import com.tradesim.model.Trade;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class MonteCarlo {
    public static void runMonteCarloParallel(Config cfg) throws Exception {
        System.out.println("Running Monte Carlo with " + String.format("%,d", cfg.numSimulations) +
                " simulations on virtual threads... (batch=" + cfg.batchSize + ")");
        List<Long> finalBalances = Collections.synchronizedList(new ArrayList<>());
        List<Double> drawdowns = Collections.synchronizedList(new ArrayList<>());
        List<Map<String, Object>> allResults = Collections.synchronizedList(new ArrayList<>());

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletionService<Map<String, Object>> cs = new ExecutorCompletionService<>(executor);
            int submitted = 0;
            int counter = 0;
            for (int start = 0; start < cfg.numSimulations; start += cfg.batchSize) {
                int size = Math.min(cfg.batchSize, cfg.numSimulations - start);
                // submit batch
                for (int i = 0; i < size; i++) {
                    long seed = new Random().nextInt(1_000_000);
                    Config configCopy = cfg.copyWithSeed(seed);
                    cs.submit(() -> runSingleSimulation(configCopy));
                    submitted++;
                }
                Instant startTime = Instant.now();
                // collect batch results
                for (int i = 0; i < size; i++) {
                    Future<Map<String, Object>> f = cs.take();
                    Map<String, Object> res = f.get();
                    allResults.add(res);
                    finalBalances.add((Long) res.get("final_balance"));
                    drawdowns.add((Double) res.get("max_drawdown"));
                    counter++;
                    if (counter % cfg.batchSize == 0 || counter == cfg.numSimulations) {
                        System.out.println("Finished " + String.format("%,d", counter) + " of " + String.format("%,d", cfg.numSimulations)+" in "+ String.format("%,d seconds", (Instant.now().getEpochSecond()-startTime.getEpochSecond())));
                    }
                }
            }
        }

        SimulationSaver.saveSimulationResults(cfg, allResults, "montecarlo_results.mpack");


        // Summary
        long hits = finalBalances.stream().filter(b -> b >= cfg.targetBalance).count();
        long bankrupt = finalBalances.stream().filter(b -> b == 0).count();
        double avg = finalBalances.stream().mapToLong(Long::longValue).average().orElse(0);
        long median = finalBalances.stream().sorted().skip(finalBalances.size()/2).findFirst().orElse(0L);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("Monte Carlo Simulation Results (" + String.format("%,d", cfg.numSimulations) + " runs):");
        System.out.println("Target balance: $" + String.format("%,d", cfg.targetBalance));
        System.out.println("Success rate: " + hits + "/" + cfg.numSimulations);
        System.out.println("Bankruptcy rate: " + bankrupt + "/" + cfg.numSimulations);
        System.out.println("Median final balance: $" + median);
        System.out.println("Average final balance: $" + (long) avg);

        Analyze.analyzeBalances(finalBalances);
    }

    public static Map<String, Object> runSingleSimulation(Config cfg) {
        List<Trade> sampleTrades = TradeGenerator.generateSampleTrades(cfg);
        Map<String, Object> sim = Simulator.simulateTrades(cfg, sampleTrades);
        List<Long> bh = (List<Long>) sim.get("balance_history");
        long finalBalance = bh.get(bh.size() - 1);
        double maxDrawdown = (Double) sim.get("max_drawdown");
        Map<String, Object> r = new HashMap<>();
        r.put("final_balance", finalBalance);
        r.put("max_drawdown", maxDrawdown);
        r.put("hit_target", finalBalance >= cfg.targetBalance);
        r.put("bankrupt", finalBalance == 0);
        if(!cfg.montyCarlo) {
            List<Map<String, Object>> tradeLog = (List<Map<String, Object>>) sim.get("trade_log");
            r.put("trade_log", tradeLog);
        }
        return r;
    }

    public static void runAndSummarizeSingleSimulation(Config cfg) {
        Map<String, Object> result = MonteCarlo.runSingleSimulation(cfg);
        List<Map<String, Object>> tradeLog = (List<Map<String, Object>>) result.get("trade_log");

//            try (PrintWriter writer = new PrintWriter("trade_log.csv")) {
//                // Write header
//                writer.println("trade_num,outcome,rrr,actual_risk_pct,risk_amount,amount,start_balance,end_balance");
//
//                for (Map<String, Object> trade : tradeLog) {
//                    writer.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
//                            trade.get("trade_num"),
//                            trade.get("outcome"),
//                            trade.get("rrr"),
//                            trade.get("actual_risk_pct"),
//                            trade.get("risk_amount"),
//                            trade.get("amount"),
//                            trade.get("start_balance"),
//                            trade.get("end_balance"));
//                }
//            }
        if (tradeLog != null && !tradeLog.isEmpty()) {
            for (Map<String, Object> trade : tradeLog) {
                int tradeNum = (int) trade.get("trade_num");
                String outcome = (String) trade.get("outcome");
                double rrr = (double) trade.get("rrr");
                double actualRiskPct = ((Number) trade.get("actual_risk_pct")).doubleValue();
                double riskAmount = ((Number) trade.get("risk_amount")).doubleValue();
                double amount = ((Number) trade.get("amount")).doubleValue();
                long startBalance = ((Number) trade.get("start_balance")).longValue();
                long endBalance = ((Number) trade.get("end_balance")).longValue();

                System.out.printf("%-10d | %-8s | %-4.1f | %-16.2f | %-12s | %-8s | %-14s | %-11s%n",
                        tradeNum,
                        outcome,
                        rrr,
                        actualRiskPct,
                        formatWithUnderscores((long) riskAmount),
                        formatWithUnderscores((long) amount),
                        formatWithUnderscores(startBalance),
                        formatWithUnderscores(endBalance));
            }
        } else {
            System.out.println("No trades found.");
        }

        // Print Summary
        System.out.println("\nSUMMARY");
        System.out.println("=======");

        System.out.printf("Final Balance : %,d%n", result.get("final_balance"));
        System.out.printf("Max Drawdown  : %.2f%n", result.get("max_drawdown"));
    }

    private static String formatWithUnderscores(long number) {
        return String.format(Locale.US, "%,d", number);
    }
}
