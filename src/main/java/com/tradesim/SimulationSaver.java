package com.tradesim;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class SimulationSaver {
    public static void saveSimulationResults(Config config, List<Map<String, Object>> results, String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> finalObject = new HashMap<>();
        finalObject.put("config", config);       // Assuming config is serializable to JSON
        finalObject.put("results", results);

//        Map<String, Object> tradeObject = new HashMap<>();
//        List<Map<String, Object>> tradeLogs = new ArrayList<>();
//        for(Map<String, Object> entry: results) {
//            tradeLogs.add((Map<String, Object>) entry.get("trade_log"));
//            entry.remove("trade_log");
//        }
//        tradeObject.put("trade_logs", tradeLogs);
//        String tradeLogsJson = mapper.writeValueAsString(finalObject);
//        try (GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream("tradeLogs.json.gz"))) {
//            gos.write(tradeLogsJson.getBytes());
//        }

        // Convert to JSON string
        String resultsJson = mapper.writeValueAsString(finalObject);
//        try (FileWriter fw = new FileWriter("results.json")) {
//            fw.write(resultsJson);
//        }
        try (GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream("results.json.gz"))) {
            gos.write(resultsJson.getBytes());
        }

        // Pack JSON string using MessagePack for compact binary
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        byte[] jsonBytes = resultsJson.getBytes(StandardCharsets.UTF_8);
        packer.packBinaryHeader(jsonBytes.length);
        packer.writePayload(jsonBytes);
        packer.close();

        // Write to binary file
        Files.write(Path.of(filePath), packer.toByteArray());
        System.out.println("Saved " + results.size() + " simulation results to: " + filePath);
    }
}
