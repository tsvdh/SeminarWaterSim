package org.watersim.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {

    @NoArgsConstructor
    @Getter @Setter
    private static class SimConfig {
        int fps;
        int seconds;
        boolean separateFiles;
        int width;
        int height;
        boolean swe;
        boolean airy;
    }

    @NoArgsConstructor
    @Getter @Setter
    private static class GlobalConfig {
        String name;
    }

    // global config variables
    public static String NAME;

    // sim config variables
    public static float TIME_STEP;
    public static float LENGTH;
    public static boolean SEPARATE_FILES;
    public static int WIDTH;
    public static int HEIGHT;
    public static boolean SWE;
    public static boolean Airy;

    // constants
    public static int CELL_SIZE = 1;
    public static float GRAVITY = 9.81f;

    // runtime variables
    public static int FRAME = 1;

    public static void readConfig() {
        var mapper = new ObjectMapper();

        Path globalConfigPath = Paths.get("grids/input/config.json");
        try (BufferedReader reader = Files.newBufferedReader(globalConfigPath)) {
            GlobalConfig globalConfig = mapper.readValue(reader, GlobalConfig.class);
            NAME = globalConfig.name;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        Path simConfigPath = Paths.get("grids/input/%s/config.json".formatted(NAME));
        try (BufferedReader reader = Files.newBufferedReader(simConfigPath)) {
            SimConfig simConfig = mapper.readValue(reader, SimConfig.class);

            TIME_STEP = 1f / simConfig.fps;
            LENGTH = simConfig.seconds;
            SEPARATE_FILES = simConfig.separateFiles;
            WIDTH = simConfig.width;
            HEIGHT = simConfig.height;
            SWE = simConfig.swe;
            Airy = simConfig.airy;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
