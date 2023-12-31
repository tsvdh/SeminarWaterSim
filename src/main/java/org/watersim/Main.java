package org.watersim;

import me.tongfei.progressbar.ProgressBar;
import org.watersim.algorithm.Simulator;
import org.watersim.grid.Grid;
import org.watersim.util.Config;
import org.watersim.util.GridBuilder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
        Config.readConfig();

        if (Config.USE_BUILDER) {
            System.out.printf("Building grids for '%s'\n".formatted(Config.NAME));
            GridBuilder.buildGrid();
        }

        System.out.printf("Simulating '%s'\n", Config.NAME);

        String outPath = "grids/output/%s/%s.txt";
        Path parentPath = Paths.get(outPath.formatted(Config.NAME, 0)).getParent();

        try {
            Files.createDirectories(parentPath);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        // delete frames
        try (Stream<Path> fileStream = Files.list(parentPath)) {
            for (Path file : fileStream.toList()) {
                Files.delete(file);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int numFrames = Math.round(Config.LENGTH / Config.TIME_STEP);
        long simStart = System.currentTimeMillis();

        try {
            BufferedWriter writer = null;
            Path fullFilePath = Paths.get(outPath.formatted(Config.NAME, "full"));
            if (!Config.SEPARATE_FILES) {
                try {
                    Files.deleteIfExists(fullFilePath);
                    writer = Files.newBufferedWriter(fullFilePath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            var simulator = new Simulator(Paths.get("grids/input/%s/data.txt".formatted(Config.NAME)));
            if (Config.SEPARATE_FILES)
                simulator.getGrid().dump(outPath.formatted(Config.NAME, 0));
            else {
                assert writer != null;
                writer.write(simulator.getGrid().toString());
                writer.write("--\n");
            }

            var bar = new ProgressBar("Sim", numFrames);

            for (; Config.FRAME <= numFrames; Config.FRAME++) {
                Grid newGrid = simulator.makeNewGrid();

                if (Config.SEPARATE_FILES)
                    newGrid.dump(outPath.formatted(Config.NAME, Config.FRAME));
                else {
                    assert writer != null;
                    writer.write(newGrid.toString());
                    writer.write("--\n");
                }

                bar.step();
                bar.refresh();
            }

            bar.close();

            if (!Config.SEPARATE_FILES) {
                assert writer != null;
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        long simTime = System.currentTimeMillis() - simStart;
        System.out.printf("Sim took: %ss (%sms per frame)\n", simTime / 1000, simTime / numFrames);

        System.exit(0);
    }
}