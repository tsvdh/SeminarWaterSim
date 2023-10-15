package org.watersim;

import org.apache.commons.lang3.tuple.Pair;

public class Main {

    public static void main(String[] args) {
        // read grid h values
        var grid = new Grid("grids/simple_water.txt");

        // set q velocities
        for (int y = 0; y <= grid.HEIGHT + 1; y++) {
            for (int x = 0; x <= grid.WIDTH + 1; x++) {
                grid.getCell(x, y).qx = 0f;
                grid.getCell(x, y).qy = 0f;
            }
        }

        System.out.println(grid);

        for (int i = 0; i < 4; i++) {
            grid = simulateTimeStep(grid);
            System.out.println(grid);
        }
    }

    public static Grid simulateTimeStep(Grid grid) {
        Grid bulk = Decomposer.decomposeBulkOnly(grid).getLeft();

        // compute u velocities
        for (int y = 1; y <= grid.HEIGHT; y++) {
            for (int x = 1; x <= grid.WIDTH; x++) {
                Cell cell = bulk.getCell(x, y);

                Pair<Float, Float> averageH = bulk.getAverageH(x, y);

                // do not compute for border
                if (x < grid.WIDTH)
                    cell.ux = averageH.getLeft() == 0 ? 0 : cell.qx / averageH.getLeft();
                if (y < grid.HEIGHT)
                    cell.uy = averageH.getRight() == 0 ? 0 : cell.qy / averageH.getRight();
            }
        }

        Grid newBulk = bulk.copy();

        // compute new bulk values
        for (int y = 1; y <= grid.HEIGHT; y++) {
            for (int x = 1; x <= grid.WIDTH; x++) {

                newBulk.getCell(x, y).h += BulkFlowComputer.computeHDerivative(x, y, bulk) * Config.TIME_STEP;

                // do not compute for border
                if (x < grid.WIDTH)
                    newBulk.getCell(x, y).ux += BulkFlowComputer.computeUXDerivative(x, y, bulk) * Config.TIME_STEP;
                if (y < grid.HEIGHT)
                    newBulk.getCell(x, y).uy += BulkFlowComputer.computeUYDerivative(x, y, bulk) * Config.TIME_STEP;
            }
        }

        // compute q velocities
        for (int y = 1; y <= grid.HEIGHT; y++) {
            for (int x = 1; x <= grid.WIDTH; x++) {
                Cell cell = newBulk.getCell(x, y);

                Pair<Float, Float> averageH = newBulk.getAverageH(x, y);

                // do not compute for border
                if (x < grid.WIDTH)
                    cell.qx = cell.ux * averageH.getLeft();
                if (y < grid.HEIGHT)
                    cell.qy = cell.uy * averageH.getRight();
            }
        }

        return newBulk;
    }
}