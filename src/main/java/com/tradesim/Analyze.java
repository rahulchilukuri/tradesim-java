package com.tradesim;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Analyze {
    public static void analyzeBalances(List<Long> finalBalances) {
        analyze(finalBalances, 10_000_000L, 1_000_000_000L);
//        System.out.println("=".repeat(80));
//        analyze(finalBalances, 100_000L, 1_000_000L);
//        analyze(finalBalances, 1_000_000L, 10_000_000L);
//        analyze(finalBalances, 10_000_000L, 100_000_000L);
//        analyze(finalBalances, 100_000_000L, 1_000_000_000L);

//        System.out.println("=".repeat(80));
//        analyze(finalBalances, 100_000L, 100_000L, 1_000_000L);
//        analyze(finalBalances, 1_000_000L,1_000_000L, 10_000_000L);
//        analyze(finalBalances, 10_000_000L,10_000_000L, 100_000_000L);
//        analyze(finalBalances, 100_000_000L,100_000_000L, 1_000_000_000L);
//        System.out.println("=".repeat(80));
    }

    private static void analyze(List<Long> finalBalances, long binSize, long minBalance, long maxBalance) {

        List<Long> filtered = finalBalances.stream().filter(b -> b > minBalance && b < maxBalance).collect(Collectors.toList());
        if (filtered.isEmpty()) {
            System.out.println("\nNo balances < " + maxBalance);
            return;
        }
        //long maxBalance = filtered.stream().mapToLong(Long::longValue).max().orElse(0);
        int bins = 10;
        long[] counts = new long[bins];
        for (long b : filtered) {
            int idx = (int) Math.min(bins - 1, b / binSize);
            counts[idx]++;
        }
        System.out.println("-".repeat(80));
        System.out.printf("\nFinal Balance Distribution (< %,d, per $%,d):%n", maxBalance, binSize);
        for (int i = 0; i < counts.length; i++) {
            long lower = i * binSize;
            long upper = (i + 1) * binSize;
            if (counts[i] > 0) {
                System.out.printf("$%,d - $%,d: %d%n", lower, upper, counts[i]);
            }
        }
    }

    private static void analyze(List<Long> finalBalances, long binSize, long balance) {
        List<Long> filtered = finalBalances.stream().filter(b -> b < balance).collect(Collectors.toList());
        if (filtered.isEmpty()) {
            System.out.println("\nNo balances < " + balance);
            return;
        }
        long maxBalance = filtered.stream().mapToLong(Long::longValue).max().orElse(0);
        int bins = 10;
        long[] counts = new long[bins];
        for (long b : filtered) {
            int idx = (int) Math.min(bins - 1, b / binSize);
            counts[idx]++;
        }
        //System.out.println("-".repeat(80));
        System.out.printf("\nFinal Balance Distribution (< %,d, per $%,d):%n", balance, binSize);
        for (int i = 0; i < counts.length; i++) {
            long lower = i * binSize;
            long upper = (i + 1) * binSize;
            if (counts[i] > 0) {
                System.out.printf("$%,d - $%,d: %d%n", lower, upper, counts[i]);
            }
        }
    }

    public static void analyzePercentiles(List<Long> finalBalances) {
        analyzePercentiles(finalBalances, 0.3, 0.8);
    }
    public static void analyzePercentiles(List<Long> finalBalances, double low, double high) {
        if (finalBalances.isEmpty()) {
            System.out.println("No balances to analyze.");
            return;
        }

        // Convert and sort
        List<Long> sorted = new ArrayList<>(finalBalances);
        Collections.sort(sorted);

        int n = sorted.size();

        long p30 = sorted.get((int) (n * low));
        long p80 = sorted.get((int) (n * high));

        System.out.printf("\nlow (%d) Percentile: $%,12d", (int)(low*100), p30);
        System.out.printf("\nhigh (%d) Percentile: $%,12d\n",(int)(high*100) ,p80);

        if (p30 == p80) {
            System.out.println("Percentile range is zero.");
            return;
        }

        // Split into buckets of $10M
        long bucketSize = 10_000_000;
        long rangeStart = (p30 / bucketSize) * bucketSize;
        long rangeEnd = ((p80 + bucketSize - 1) / bucketSize) * bucketSize;

        int numBuckets = (int) ((rangeEnd - rangeStart) / bucketSize);
        int[] bucketCounts = new int[numBuckets];

        for (long val : finalBalances) {
            if (val >= rangeStart && val < rangeEnd) {
                int index = (int) ((val - rangeStart) / bucketSize);
                bucketCounts[index]++;
            }
        }

        // Print results
        System.out.printf("\nDistribution between %dth–%dth percentile (in $10M buckets):\n", (int)(low*100), (int)(high*100));
        for (int i = 0; i < numBuckets; i++) {
            if(bucketCounts[i] == 0) continue;
            long from = rangeStart + i * bucketSize;
            long to = from + bucketSize;
            System.out.printf("$%,12d – $%,12d : %,d\n", from, to - 1, bucketCounts[i]);
        }
    }
}
