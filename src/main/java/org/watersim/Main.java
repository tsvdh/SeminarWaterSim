package org.watersim;

import me.tongfei.progressbar.ProgressBar;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
        String name = "medium";

        Config.readConfig(Paths.get("grids/input/%s/config.json".formatted(name)));

        System.out.printf("Simulating '%s'\n", name);

        String outPath = "grids/output/%s/%s.txt";
        Path parentPath = Paths.get(outPath.formatted(name, 0)).getParent();

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
            Path fullFilePath = Paths.get(outPath.formatted(name, "full"));
            if (!Config.SEPARATE_FILES) {
                try {
                    Files.deleteIfExists(fullFilePath);
                    writer = Files.newBufferedWriter(fullFilePath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            var simulator = new Simulator(Paths.get("grids/input/%s/data.txt".formatted(name)));
            if (Config.SEPARATE_FILES)
                simulator.getGrid().dump(outPath.formatted(name, 0));
            else {
                assert writer != null;
                writer.write(simulator.getGrid().toString());
                writer.write("--\n");
            }

            try (var bar = new ProgressBar("Sim", numFrames)) {
                for (; Config.FRAME <= numFrames; Config.FRAME++) {
                    Grid newGrid = simulator.makeNewGrid();

                    if (Config.SEPARATE_FILES)
                        newGrid.dump(outPath.formatted(name, Config.FRAME));
                    else {
                        assert writer != null;
                        writer.write(newGrid.toString());
                        writer.write("--\n");
                    }

                    bar.step();
                    bar.refresh();
                }
            }
            if (!Config.SEPARATE_FILES) {
                assert writer != null;
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        long simTime = System.currentTimeMillis() - simStart;
        System.out.printf("Sim took: %ss (%sms per frame)\n", simTime / 1000, simTime / numFrames);
    }
}