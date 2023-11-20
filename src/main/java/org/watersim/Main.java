package org.watersim;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
        String name = "large_2d";

        Config.readConfig(Paths.get("grids/input/%s/config.txt".formatted(name)));

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

        var simulator = new Simulator(Paths.get("grids/input/%s/data.txt".formatted(name)));
        simulator.getGrid().dump(outPath.formatted(name, 0));

        int numFrames = Math.round(Config.LENGTH / Config.TIME_STEP);
        long simStart = System.currentTimeMillis();

        try (var bar = new ProgressBar("Sim", numFrames)) {
            for (; Config.FRAME <= numFrames; Config.FRAME++) {
                Grid newGrid = simulator.makeNewGrid();
                newGrid.dump(outPath.formatted(name, Config.FRAME));
                bar.step();
                bar.refresh();
            }
        }

        long simTime = System.currentTimeMillis() - simStart;

        System.out.printf("Sim took: %ss (%sms per frame)\n", simTime / 1000, simTime / numFrames);
    }
}