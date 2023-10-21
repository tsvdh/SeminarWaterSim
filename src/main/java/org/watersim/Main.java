package org.watersim;

import org.apache.commons.lang3.tuple.Pair;

public class Main {

    public static void main(String[] args) {
        // read grid h values
        var grid = new Grid("grids/simple_correct/input.txt");

        // set q velocities
        for (int y = 0; y <= grid.HEIGHT + 1; y++) {
            for (int x = 0; x <= grid.WIDTH + 1; x++) {
                grid.getCell(x, y).qx = 0f;
                grid.getCell(x, y).qy = 0f;
            }
        }

        Grid prevGrid = grid.copy();

        String outPath = "grids/simple_correct/out%s.txt";
        grid.dump(outPath.formatted(0));

        for (; Config.FRAME <= 100; Config.FRAME++) {
            Grid newGrid = simulateTimeStep(prevGrid, grid);
            prevGrid = grid;
            grid = newGrid;

            grid.dump(outPath.formatted(Config.FRAME));
        }
    }

    public static Grid simulateTimeStep(Grid prevGrid, Grid grid) {
        Grid prevBulk = Decomposer.decomposeBulkOnly(prevGrid).getLeft();
        Grid bulk = Decomposer.decomposeBulkOnly(grid).getLeft();

        // compute u velocities
        for (int y = 1; y <= grid.HEIGHT; y++) {
            for (int x = 1; x <= grid.WIDTH; x++) {
                Cell cell = bulk.getCell(x, y);

                Pair<Float, Float> averageH = prevBulk.getUpwindH(x, y);

                // do not compute for border
                if (x < grid.WIDTH)
                    cell.ux = averageH.getLeft() == 0 ? 0 : cell.qx / averageH.getLeft();
                if (y < grid.HEIGHT)
                    cell.uy = averageH.getRight() == 0 ? 0 : cell.qy / averageH.getRight();
            }
        }

        Grid newBulk = new Grid(grid.WIDTH, grid.HEIGHT);

        // compute new bulk values
        for (int y = 1; y <= grid.HEIGHT; y++) {
            for (int x = 1; x <= grid.WIDTH; x++) {

                // newBulk.getCell(x, y).h += BulkFlowComputer.computeHDerivative(x, y, bulk) * Config.TIME_STEP;

                // do not compute for border
                if (x < grid.WIDTH)
                    newBulk.getCell(x, y).ux = bulk.getCell(x, y).ux + BulkFlowComputer.computeUXDerivative(x, y, bulk) * Config.TIME_STEP;
                if (y < grid.HEIGHT)
                    newBulk.getCell(x, y).uy = bulk.getCell(x, y).uy + BulkFlowComputer.computeUYDerivative(x, y, bulk) * Config.TIME_STEP;
            }
        }

        // compute q velocities
        for (int y = 1; y <= grid.HEIGHT; y++) {
            for (int x = 1; x <= grid.WIDTH; x++) {
                Cell cell = newBulk.getCell(x, y);

                Pair<Float, Float> averageH = bulk.getUpwindH(x, y);

                // do not compute for border
                if (x < grid.WIDTH)
                    cell.qx = cell.ux * averageH.getLeft();
                if (y < grid.HEIGHT)
                    cell.qy = cell.uy * averageH.getRight();
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
}