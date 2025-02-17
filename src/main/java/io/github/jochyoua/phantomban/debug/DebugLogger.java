package io.github.jochyoua.phantomban.debug;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.bukkit.Bukkit;

import java.util.logging.Level;

public class DebugLogger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<Map.Entry<Level, String>> logEntries = new TreeSet<>((e1, e2) -> {
        String timestamp1 = e1.getValue().substring(0, 19);
        String timestamp2 = e2.getValue().substring(0, 19);
        return timestamp1.compareTo(timestamp2);
    });

    private DebugLogger() {
        throw new UnsupportedOperationException("Cannot instantiate logging class.");
    }

    public static void logMessage(Level level, String message) {
        if (level.equals(Level.WARNING) || level.equals(Level.SEVERE)) {
            Bukkit.getLogger().warning(message);
        }
        String timeStamp = LocalDateTime.now().format(formatter);
        StackTraceElement element = Thread.currentThread().getStackTrace()[3];

        String logMessage = String.format("%s [%s] %s.%s(%s:%d) -%n %s", timeStamp, level.getName(),
                element.getClassName(), element.getMethodName(), element.getFileName(), element.getLineNumber(), message);

        logEntries.add(new AbstractMap.SimpleEntry<>(level, logMessage));
    }

    public static void saveToFile(String filePathInfo, String filePathSevere, String filePathWarning) {
        writeEntriesToFile(filePathInfo, Level.INFO);
        writeEntriesToFile(filePathSevere, Level.SEVERE);
        writeEntriesToFile(filePathWarning, Level.WARNING);
        logEntries.clear();
    }

    private static void writeEntriesToFile(String filePath, Level level) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Map.Entry<Level, String> entry : logEntries) {
                if (entry.getKey() == level) {
                    writer.write(entry.getValue());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to write log to file: " + e.getMessage());
        }
    }
}
