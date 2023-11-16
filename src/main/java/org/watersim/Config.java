package org.watersim;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class Config {

    public static float TIME_STEP;
    public static float LENGTH;
    public static int CELL_SIZE = 1;
    public static float GRAVITY = 9.81f;

    public static int FRAME = 1;

    public static void readConfig(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            List<String> lines = reader.lines().toList();
            TIME_STEP = 1f / Integer.parseInt(lines.get(0).trim());
            LENGTH = Integer.parseInt(lines.get(1).trim());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
