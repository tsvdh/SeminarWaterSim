package org.watersim;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class Config {

    @NoArgsConstructor
    @Getter @Setter
    private static class ConfigFile {
        int fps;
        int seconds;
        boolean separateFiles;
    }

    // config variables
    public static float TIME_STEP;
    public static float LENGTH;
    public static boolean SEPARATE_FILES;

    // constants
    public static int CELL_SIZE = 1;
    public static float GRAVITY = 9.81f;

    // runtime variables
    public static int FRAME = 1;

    public static void readConfig(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            var mapper = new ObjectMapper();
            ConfigFile configFile = mapper.readValue(reader, ConfigFile.class);

            TIME_STEP = 1f / configFile.fps;
            LENGTH = configFile.seconds;
            SEPARATE_FILES = configFile.separateFiles;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
