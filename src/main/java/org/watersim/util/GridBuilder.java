package org.watersim.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.watersim.grid.Cell;
import org.watersim.grid.Grid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.watersim.util.Config.HEIGHT;
import static org.watersim.util.Config.WIDTH;

public class GridBuilder {

    @NoArgsConstructor
    @Getter @Setter
    private static class BuilderUnit {
        int[] topLeft;
        int[] bottomRight;
        int height;

        boolean coorsInArea(int x, int y) {
            return x >= topLeft[0] && x <= bottomRight[0]
                    && y >= topLeft[1] && y <= bottomRight[1];
        }
    }

    @NoArgsConstructor
    @Getter @Setter
    private static class BuilderConfig {
        List<BuilderUnit> water;
        List<BuilderUnit> wall;
    }

    public static void buildGrid() {
        var mapper = new ObjectMapper();

        Path builderPath = Paths.get("grids/input/%s/builder.json".formatted(Config.NAME));
        BuilderConfig config;
        try (BufferedReader reader = Files.newBufferedReader(builderPath)) {
            config = mapper.readValue(reader, BuilderConfig.class);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        var waterHeights = new Grid();
        var wallHeights = new Grid();

        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
                Cell waterCell = waterHeights.getCell(x, y);
                Cell wallCell = wallHeights.getCell(x, y);

                for (BuilderUnit unit : config.water) {
                    if (unit.coorsInArea(x, y)) {
                        waterCell.h = unit.height;
                    }
                }
                for (BuilderUnit unit : config.wall) {
                    if (unit.coorsInArea(x, y)) {
                        wallCell.h = unit.height;
                        if (unit.height > 0) {
                            waterCell.h = 0;
                        }
                    }
                }
            }
        }

        Path dataPath = Paths.get("grids/input/%s/data.txt".formatted(Config.NAME));
        try {
            Files.deleteIfExists(dataPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(dataPath)) {
            writer.write(waterHeights.toString(Grid.DataType.H));
            writer.write("-\n");
            writer.write(wallHeights.toString(Grid.DataType.H));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
