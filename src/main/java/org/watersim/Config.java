package org.watersim;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class Config {

    public static float TIME_STEP;
    public static int CELL_SIZE = 1;
    public static float GRAVITY = 9.81f;

    public static int FRAME = 1;

    public static void readConfig(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            TIME_STEP = 1f / Integer.parseInt(reader.lines().toList().get(0).trim());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
