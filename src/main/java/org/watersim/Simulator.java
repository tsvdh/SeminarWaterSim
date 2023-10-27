package org.watersim;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Simulator {

    private Grid prevGrid;
    private Grid grid;

    private final Grid wallGrid;

    public Simulator(Path path) {
        // read grid values
        StringBuilder water = new StringBuilder();
        StringBuilder walls = new StringBuilder();

        try (var reader = Files.newBufferedReader(path)) {
            boolean readingWater = true;

            for (String line : reader.lines().toList()) {
                if (readingWater) {
                    if (line.equals("-"))
                        readingWater = false;
                    else
                        water.append(line);
                } else {
                    walls.append(line);
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        grid = new Grid(water.toString());
        prevGrid = grid.copy();

        wallGrid = new Grid(walls.toString());
    }

    public Grid getGrid() {
        return grid;
    }

    public Grid makeNewGrid() {
        Grid prevBulk = Decomposer.decomposeBulkOnly(prevGrid).getLeft();
        Grid bulk = Decomposer.decomposeBulkOnly(grid).getLeft();

        // compute u velocities
        for (int y = 1; y <= grid.HEIGHT; y++) {
            for (int x = 1; x <= grid.WIDTH; x++) {
                Cell cell = bulk.getCell(x, y);

                Pair<Float, Float> upwindH = prevBulk.getUpwindH(x, y);

                // do not compute for walls
                if (canFlowRight(x, y))
                    cell.ux = upwindH.getLeft() == 0 ? 0 : cell.qx / upwindH.getLeft();
                if (canFlowDown(x, y))
                    cell.uy = upwindH.getRight() == 0 ? 0 : cell.qy / upwindH.getRight();
            }
        }

        Grid newBulk = new Grid(grid.WIDTH, grid.HEIGHT);

        // compute new bulk values
        for (int y = 1; y <= grid.HEIGHT; y++) {
            for (int x = 1; x <= grid.WIDTH; x++) {
                // do not compute for walls
                if (canFlowRight(x, y)) {
                    float newU = bulk.getCell(x, y).ux + BulkFlowComputer.computeUXDerivative(x, y, bulk) * Config.TIME_STEP;
                    newBulk.getCell(x, y).ux = clampU(newU);
                }
                if (canFlowDown(x, y)) {
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

                // do not compute for walls
                if (canFlowRight(x, y))
                    cell.qx = cell.ux * upwindH.getLeft();
                if (canFlowDown(x, y))
                    cell.qy = cell.uy * upwindH.getRight();
            }
        }

        // update heights with flow divergence
        for (int y = 1; y <= grid.HEIGHT; y++) {
            for (int x = 1; x <= grid.WIDTH; x++) {
                // TODO: perhaps only for non walls
                Cell cell = newBulk.getCell(x, y);
                Cell leftCell = newBulk.getCell(x - 1, y);
                Cell upCell = newBulk.getCell(x, y - 1);

                float divergence = (cell.qx - leftCell.qx) / Config.CELL_SIZE + (cell.qy - upCell.qy) / Config.CELL_SIZE;
                divergence *= -1;

                cell.h = bulk.getCell(x, y).h + divergence * Config.TIME_STEP;
            }
        }

        prevGrid = bulk;
        grid = newBulk;

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

    public boolean canFlowRight(int x, int y) {
        return wallGrid.getCell(x, y).h == 0 && wallGrid.getCell(x + 1, y).h == 0;
    }

    public boolean canFlowDown(int x, int y) {
        return wallGrid.getCell(x, y).h == 0 && wallGrid.getCell(x, y + 1).h == 0;
    }
}
