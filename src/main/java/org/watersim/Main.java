package org.watersim;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        String name = "simple_1d";
        int numFrames = 20;

        String outPath = "grids/output/%s/%s.txt";

        Path parentPath = Paths.get(outPath.formatted(name, 0)).getParent();
        try {
            Files.createDirectories(parentPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var simulator = new Simulator(Paths.get("grids/input/%s.txt".formatted(name)));

        simulator.getGrid().dump(outPath.formatted(name, 0));

        for (; Config.FRAME <= numFrames; Config.FRAME++) {
            Grid newGrid = simulator.makeNewGrid();

            newGrid.dump(outPath.formatted(name, Config.FRAME));
        }
    }
}