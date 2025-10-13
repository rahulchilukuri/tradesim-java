package com.tradesim;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.core.MessagePack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.lang.String.format;

public class SimulationLoader {
    private static final ObjectMapper mapper = new ObjectMapper();
    public static void main(String[] args) throws Exception {
        summarizeFromFile("montecarlo_results.mpack");
    }

    public static Map<String, Object> loadSimulationResults(String filePath) throws IOException {
        byte[] data = Files.readAllBytes(Path.of(filePath));
        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
        int length = unpacker.unpackBinaryHeader();
        byte[] jsonBytes = unpacker.readPayload(length);
        unpacker.close();

        String json = new String(jsonBytes, StandardCharsets.UTF_8);
        return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    public static void summarizeFromFile(String filePath) throws Exception {
        Map<String, Object> loaded = loadSimulationResults(filePath);

        // Deserialize config
        Config cfg = mapper.convertValue(loaded.get("config"), Config.class);

        // Get results
        List<Map<String, Object>> results = (List<Map<String, Object>>) loaded.get("results");
        System.out.println(results.size());

        // Extract final balances
        List<Long> finalBalances = results.stream()
                .map(r -> ((Number) r.get("final_balance")).longValue())
                .toList();

        int count = finalBalances.size();
        int hits = 0;
        int bankrupt = 0;
        long sum = 0;
        long maximum = finalBalances.get(0);
        long minimum = finalBalances.get(0);

        long[] sorted = new long[count]; // For median
        for (int i = 0; i < count; i++) {
            long val = finalBalances.get(i);
            sorted[i] = val;
            sum += val;
            minimum = Math.min(minimum, val);
            maximum = Math.max(maximum, val);
            if (val >= cfg.targetBalance) hits++;
            if (val == 0) bankrupt++;
        }

        Arrays.sort(sorted);
        long median = sorted[count / 2];
        double avg = (double) sum / count;

        System.out.println("\n" + "=".repeat(80));
//        System.out.println(format("Monte Carlo Simulation Results (%,d runs)) :", cfg.numSimulations));
//        System.out.println(format("Target balance: $%,d", cfg.targetBalance));
//        System.out.println(format("Success rate: %,d/%,d", hits, cfg.numSimulations));
//        System.out.println(format("Bankruptcy rate: %,d/%,d", bankrupt, cfg.numSimulations));
//        System.out.println(format("Median final balance: $%,d", median));
//        System.out.println(format("Average final balance: $%,d", (long) avg));
        System.out.printf("\nMonte Carlo Simulation Results (%,d runs)) :%n", cfg.numSimulations);
        System.out.printf("\nTarget balance: $%,d", cfg.targetBalance);
        System.out.printf("\nSuccess rate: %,d/%,d", hits, cfg.numSimulations);
        System.out.printf("\nBankruptcy rate: %,d/%,d", bankrupt, cfg.numSimulations);
        System.out.printf("\nMinimum final balance: $%,d", minimum);
        System.out.printf("\nMedian final balance: $%,d", median);
        System.out.printf("\nMaximum final balance: $%,d", maximum);
        System.out.printf("\nAverage final balance: $%,d", (long) avg);
        System.out.println("\n" + "=".repeat(80));

        // Optional deeper analysis
        //Analyze.analyzeBalances(finalBalances);

//        System.out.println("\n" + "=".repeat(80));
//        Analyze.analyzePercentiles(finalBalances);
//
//        System.out.println("\n" + "=".repeat(80));
//        Analyze.analyzePercentiles(finalBalances, .40, .60);
//
//        System.out.println("\n" + "=".repeat(80));
//        Analyze.analyzePercentiles(finalBalances, .40, .80);
//
//        System.out.println("\n" + "=".repeat(80));
//        Analyze.analyzePercentiles(finalBalances, .10, .90);
//
//        System.out.println("\n" + "=".repeat(80));
//        Analyze.analyzePercentiles(finalBalances, .60, .90);

        System.out.println("\n" + "=".repeat(80));
        Analyze.analyzePercentiles(finalBalances, .72, 0.98);
    }

}
