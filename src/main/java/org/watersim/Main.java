package org.watersim;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        String name = "simple_1d";
        int numFrames = 300;

        // read grid h values
        var grid = new Grid("grids/input/%s.txt".formatted(name));

        // set q velocities
        for (int y = 0; y <= grid.HEIGHT + 1; y++) {
            for (int x = 0; x <= grid.WIDTH + 1; x++) {
                grid.getCell(x, y).qx = 0f;
                grid.getCell(x, y).qy = 0f;
            }
        }

        Grid prevGrid = grid.copy();

        String outPath = "grids/output/%s/%s.txt";

        Path parentPath = Paths.get(outPath.formatted(name, 0)).getParent();
        try {
            Files.createDirectories(parentPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        grid.dump(outPath.formatted(name, 0));

        for (; Config.FRAME <= numFrames; Config.FRAME++) {
            Grid newGrid = simulateTimeStep(prevGrid, grid);
            prevGrid = grid;
            grid = newGrid;

            grid.dump(outPath.formatted(name, Config.FRAME));
        }
    }

    public static Grid simulateTimeStep(Grid prevGrid, Grid grid) {
        Grid prevBulk = Decomposer.decomposeBulkOnly(prevGrid).getLeft();
        Grid bulk = Decomposer.decomposeBulkOnly(grid).getLeft();

        // compute u velocities
        for (int y = 1; y <= grid.HEIGHT; y++) {
            for (int x = 1; x <= grid.WIDTH; x++) {
                Cell cell = bulk.getCell(x, y);

                Pair<Float, Float> upwindH = prevBulk.getUpwindH(x, y);

                // do not compute for border
                if (x < grid.WIDTH)
                    cell.ux = upwindH.getLeft() == 0 ? 0 : cell.qx / upwindH.getLeft();
                if (y < grid.HEIGHT)
                    cell.uy = upwindH.getRight() == 0 ? 0 : cell.qy / upwindH.getRight();
            }
        }

        Grid newBulk = new Grid(grid.WIDTH, grid.HEIGHT);

        // compute new bulk values
        for (int y = 1; y <= grid.HEIGHT; y++) {
            for (int x = 1; x <= grid.WIDTH; x++) {
                // do not compute for border
                if (x < grid.WIDTH) {
                    float newU = bulk.getCell(x, y).ux + BulkFlowComputer.computeUXDerivative(x, y, bulk) * Config.TIME_STEP;
                    newBulk.getCell(x, y).ux = clampU(newU);
                }
                if (y < grid.HEIGHT) {
                    float newU = bulk.getCell(x, y).uy + BulkFlowComputer.computeUYDerivative(x, y, bulk) * Config.TIME_STEP;
                    newBulk.getCell(x, y).uy = clampU(newU);
                }
            }
        }

        // compute q velocities
        for (int y = 1; y <= grid.HEIGHT; y++) {
            for (int x = 1; x <= grid.WIDTH; x++) {
                Cell cell = newBulk.getCell(x, y);

                Pair<Float, Float> upwindH = bulk.getUpwindH(x, y);

                // do not compute for border
                if (x < grid.WIDTH)
                    cell.qx = cell.ux * upwindH.getLeft();
                if (y < grid.HEIGHT)
                    cell.qy = cell.uy * upwindH.getRight();
            }
        }

        // update heights with flow divergence
        for (int y = 1; y <= grid.HEIGHT; y++) {
            for (int x = 1; x <= grid.WIDTH; x++) {
                Cell cell = newBulk.getCell(x, y);
                Cell leftCell = newBulk.getCell(x - 1, y);
                Cell upCell = newBulk.getCell(x, y - 1);

                float divergence = (cell.qx - leftCell.qx) / Config.CELL_SIZE + (cell.qy - upCell.qy) / Config.CELL_SIZE;
                divergence *= -1;

                cell.h = bulk.getCell(x, y).h + divergence * Config.TIME_STEP;
            }
        }

        return newBulk;
    }

    private static float clampU(float u) {
        float uMax = Config.CELL_SIZE / (4 * Config.TIME_STEP);

        if (u > 0) {
            return Math.min(u, uMax);
        }
        if (u < 0) {
            return -clampU(-u);
        }
        return 0;
    }
}