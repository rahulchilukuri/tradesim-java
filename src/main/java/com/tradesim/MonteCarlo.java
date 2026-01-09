package com.tradesim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradesim.model.Trade;

import java.io.PrintWriter;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class MonteCarlo {
    static String dashbar = "=".repeat(80);

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
                        System.out.println("Finished " + String.format("%,d", counter) + " of "
                                + String.format("%,d", cfg.numSimulations) + " in " + String.format("%,d seconds",
                                        (Instant.now().getEpochSecond() - startTime.getEpochSecond())));
                    }
                }
            }
        }

        SimulationSaver.saveSimulationResults(cfg, allResults, "montecarlo_results.mpack");

        // Summary
        long hits = finalBalances.stream().filter(b -> b >= cfg.targetBalance).count();
        long bankrupt = finalBalances.stream().filter(b -> b == 0).count();
        double avg = finalBalances.stream().mapToLong(Long::longValue).average().orElse(0);
        long median = finalBalances.stream().sorted().skip(finalBalances.size() / 2).findFirst().orElse(0L);

        System.out.printf("%n%s%n", dashbar);
        System.out.printf("Trade Parameters%n");
        System.out.printf("Min Risk Reward  : %d%n", (int) cfg.minRr);
        System.out.printf("Max Risk Reward  : %d%n", (int) cfg.maxRr);
        System.out.printf("Min Win Rate     : %.2f%%%n", 100 * cfg.winRateLow);
        System.out.printf("Max Win Rate     : %.2f%%%n%n", 100 * cfg.winRateHigh);
        System.out.println("Monte Carlo Simulation Results (" + String.format("%,d", cfg.numSimulations) + " runs):");
        System.out.println("Target balance: $" + String.format("%,d", cfg.targetBalance));
        System.out.println("Success rate: " + hits + "/" + cfg.numSimulations);
        System.out.println("Bankruptcy rate: " + bankrupt + "/" + cfg.numSimulations);
        System.out.println("Median final balance: $" + median);
        System.out.println("Average final balance: $" + (long) avg);
        System.out.printf("%s%n", dashbar);

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
        r.put("sample_trades", sampleTrades);
        if (!cfg.montyCarlo) {
            List<Map<String, Object>> tradeLog = (List<Map<String, Object>>) sim.get("trade_log");
            r.put("trade_log", tradeLog);
        }
        return r;
    }

    public static void runAndSummarizeSingleSimulation(Config cfg) throws Exception {
        Map<String, Object> result = MonteCarlo.runSingleSimulation(cfg);
        List<Map<String, Object>> tradeLog = (List<Map<String, Object>>) result.get("trade_log");
        int numWins = tradeLog.size();
        Map<Double, Integer> atMap = new HashMap<>();

        if (tradeLog != null && !tradeLog.isEmpty()) {
            try (PrintWriter fileWriter = new PrintWriter("trade_log.csv")) {
                String headerRow = "%-10s | %-8s | %-4s | %-16s | %-12s | %-12s | %-14s | %-11s%n".formatted(
                        "trade_num", "outcome", "rrr", "actual_risk_pct", "risk_amount",
                        "amount", "start_balance", "end_balance");
                System.out.printf(headerRow);

                // Print Summary
                fileWriter.println("\nSUMMARY");
                fileWriter.println("=======");

                fileWriter.printf("Min RR, %d%n", (int) cfg.minRr);
                fileWriter.printf("Max RR, %d%n", (int) cfg.maxRr);
                fileWriter.printf("Min WinRate, %.2f%%%n", 100 * cfg.winRateLow);
                fileWriter.printf("Max WinRate, %.2f%%%n", 100 * cfg.winRateHigh);
                fileWriter.printf("Final Balance,\"%,d\"%n", (long) result.get("final_balance"));

                fileWriter.printf("=======%n%n");

                fileWriter
                        .println("trade_num,outcome,rrr,actual_risk_pct,risk_amount,amount,start_balance,end_balance");
                String print_pattern = "%-10d | %-8s | %-4.1f | %-16.2f | %-12s | %-12s | %-14s | %-11s%n";
                String csv_pattern = "%-10d , %-8s , %-4.1f , %-16.2f , %-12d , %-12d , %-14d , %-11d%n";
                for (Map<String, Object> trade : tradeLog) {
                    int tradeNum = (int) trade.get("trade_num");
                    String outcome = (String) trade.get("outcome");
                    double rrr = (double) trade.get("rrr");
                    if (outcome.equals("Win"))
                        atMap.put(rrr, atMap.getOrDefault(rrr, 0) + 1);
                    double actualRiskPct = ((Number) trade.get("actual_risk_pct")).doubleValue();
                    double riskAmount = ((Number) trade.get("risk_amount")).doubleValue();
                    double amount = ((Number) trade.get("amount")).doubleValue();
                    long startBalance = ((Number) trade.get("start_balance")).longValue();
                    long endBalance = ((Number) trade.get("end_balance")).longValue();

                    if (startBalance >= endBalance) {
                        numWins--;
                    }
                    String row = print_pattern.formatted(tradeNum,
                            outcome,
                            rrr,
                            actualRiskPct,
                            formatWithUnderscores((long) riskAmount),
                            formatWithUnderscores((long) amount),
                            formatWithUnderscores(startBalance),
                            formatWithUnderscores(endBalance));
                    System.out.printf(row);

                    fileWriter.printf(Locale.US, csv_pattern, tradeNum,
                            outcome,
                            rrr,
                            actualRiskPct,
                            (long) riskAmount,
                            (long) amount,
                            startBalance,
                            endBalance);
                }
            }
        } else {
            System.out.println("No trades found.");
        }

        if (cfg.printTradeSamples) {
            // List<Trade> sampleTrades = (List<Trade>) result.get("sample_trades");
            // for(Trade sample: sampleTrades) {
            // System.out.println(sample.toString());
            // }

            // Sort by count (descending), then rrr (descending)
            List<Map.Entry<Double, Integer>> sortedEntries = atMap.entrySet()
                    .stream()
                    .sorted(Comparator
                            .comparing(Map.Entry::getKey))
                    // .comparing(Map.Entry::getKey, Comparator.reverseOrder()))
                    // .comparing(Map.Entry<Double, Integer>::getValue, Comparator.reverseOrder())
                    // .thenComparing(Map.Entry::getKey)) // uses Trade's compareTo()
                    .toList();

            // Optionally put in LinkedHashMap to preserve order
            Map<Double, Integer> sortedMap = new LinkedHashMap<>();
            for (Map.Entry<Double, Integer> entry : sortedEntries) {
                sortedMap.put(entry.getKey(), entry.getValue());
            }

            // Print result
            System.out.printf("%nOut of %d trades, %d unique trades from shuffled samples%n", cfg.numTrades,
                    atMap.size());
            System.out.printf("%-8s | %3s", "Trade", "Count");
            System.out.printf("%n%s%n", dashbar.replace("=", "-"));
            sortedMap.forEach((trade, count) -> System.out.printf("%-8s | %3d%n", trade, count));
        }

        // Print Summary
        System.out.println("\nSUMMARY");
        System.out.printf("%n%s%n", dashbar);
        System.out.printf("Total Wins       : %d%n", numWins);
        System.out.printf("Total Trades     : %d%n", cfg.numTrades);
        System.out.printf("Win %%            : %.2f%n", 100 * ((double) numWins / cfg.numTrades));
        System.out.printf("Min Risk Reward  : %d%n", (int) cfg.minRr);
        System.out.printf("Max Risk Reward  : %d%n", (int) cfg.maxRr);
        System.out.printf("Min Win Rate     : %.2f%%%n", 100 * cfg.winRateLow);
        System.out.printf("Max Win Rate     : %.2f%%%n", 100 * cfg.winRateHigh);
        System.out.printf("Max Drawdown     : %.2f%%%n", result.get("max_drawdown"));
        System.out.printf("Final Balance    : %,d%n", result.get("final_balance"));
        System.out.printf("Max Drawdown     : %.2f%%%n", result.get("max_drawdown"));
        System.out.printf("%s%n", dashbar);
    }

    private static String formatWithUnderscores(long number) {
        return String.format(Locale.US, "%,d", number);
    }
}
