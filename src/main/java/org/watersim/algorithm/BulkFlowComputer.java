package org.watersim.algorithm;

import org.apache.commons.lang3.tuple.Pair;
import org.watersim.grid.Cell;
import org.watersim.util.Config;
import org.watersim.grid.Grid;
import org.watersim.grid.WallGrid;

import java.util.function.Function;

public class BulkFlowComputer {

    public static float computeUXDerivative(int x, int y, Grid bulk) {
        Cell curCell = bulk.getCell(x, y);
        Cell leftCell = bulk.getCell(x - 1, y);
        Cell rightCell = bulk.getCell(x + 1, y);
        Cell upCell = bulk.getCell(x, y - 1);
        Cell downCell = bulk.getCell(x, y + 1);
        Cell topRightCell = bulk.getCell(x + 1, y - 1);

        Pair<Float, Float> upwindH = bulk.getUpwindH(x, y);

        float firstPart = 0;
        if (upwindH.getLeft() != 0) {
            float goingRight = (bulk.getAverageQ(x, y).getLeft() / upwindH.getLeft()) * ((curCell.ux - leftCell.ux) / Config.CELL_SIZE);
            float goingLeft = (bulk.getAverageQ(x + 1, y).getLeft() / upwindH.getLeft()) * ((rightCell.ux - curCell.ux) / Config.CELL_SIZE);

            if (curCell.ux >= 0) {
                firstPart += goingRight;
            }
            if (curCell.ux <= 0) {
                firstPart += goingLeft;
            }
        }

        float secondPart = 0;
        if (upwindH.getLeft() != 0) {

            Function<Float, Float> secondPartAbove = (downFlow) -> (downFlow / upwindH.getLeft()) * ((curCell.ux - upCell.ux) / Config.CELL_SIZE);

            if (curCell.ux >= 0 && upCell.qy > 0) {
                secondPart += secondPartAbove.apply(upCell.qy);
            }

            if (curCell.ux <= 0 && topRightCell.qy > 0) {
                secondPart += secondPartAbove.apply(topRightCell.qy);
            }

            Function<Float, Float> secondPartBelow = (upFlow) -> ((upFlow / upwindH.getLeft()) * ((downCell.ux - curCell.ux) / Config.CELL_SIZE));

            if (curCell.ux >= 0 && curCell.qy < 0) {
                secondPart += secondPartBelow.apply(curCell.qy);
            }

            if (curCell.ux <= 0 && rightCell.qy < 0) {
                secondPart += secondPartBelow.apply(rightCell.qy);
            }
        }

        float thirdPart = Config.GRAVITY * (bulk.getCell(x + 1, y).h - curCell.h) / Config.CELL_SIZE;

        return -1 * (firstPart + secondPart + thirdPart);
    }

    public static float computeUYDerivative(int x, int y, Grid bulk) {
        @SuppressWarnings("DuplicatedCode")
        Cell curCell = bulk.getCell(x, y);
        Cell leftCell = bulk.getCell(x - 1, y);
        Cell rightCell = bulk.getCell(x + 1, y);
        Cell upCell = bulk.getCell(x, y - 1);
        Cell downCell = bulk.getCell(x, y + 1);
        Cell downLeftCell = bulk.getCell(x - 1, y + 1);

        Pair<Float, Float> upwindH = bulk.getUpwindH(x, y);

        float secondPart = 0;
        if (upwindH.getRight() != 0) {
            float goingDown = (bulk.getAverageQ(x, y).getRight() / upwindH.getRight()) * ((curCell.uy - upCell.uy) / Config.CELL_SIZE);
            float goingUp = (bulk.getAverageQ(x, y + 1).getRight() / upwindH.getRight()) * ((downCell.uy - curCell.uy) / Config.CELL_SIZE);

            if (curCell.uy >= 0) {
               secondPart += goingDown;
            }
            if (curCell.uy <= 0) {
               secondPart += goingUp;
            }
        }

        float firstPart = 0;
        if (upwindH.getRight() != 0) {

            Function<Float, Float> firstLeftPart = (rightFlow) -> ((rightFlow / upwindH.getRight()) * ((curCell.uy - leftCell.uy) / Config.CELL_SIZE));

            if (curCell.uy >= 0 && leftCell.qx > 0) {
                firstPart += firstLeftPart.apply(leftCell.qx);
            }
            if (curCell.uy <= 0 && downLeftCell.qx > 0) {
                firstPart += firstLeftPart.apply(downLeftCell.qx);
            }

            Function<Float, Float> firstRightPart = (leftFlow) -> ((leftFlow / upwindH.getRight()) * ((rightCell.uy - curCell.uy ) / Config.CELL_SIZE));

            if (curCell.uy >= 0 && curCell.qx < 0) {
                firstPart += firstRightPart.apply(curCell.qx);
            }
            if (curCell.uy <= 0 && downCell.qx < 0) {
                firstPart += firstRightPart.apply(downCell.qx);
            }
        }

        float thirdPart = Config.GRAVITY * (bulk.getCell(x, y + 1).h - curCell.h) / Config.CELL_SIZE;

        return -1 * (firstPart + secondPart + thirdPart);
    }

    public static Grid computeNewBulkUAndQ(Grid grid, Grid bulk, WallGrid wallGrid) {
        Grid newBulk = new Grid(grid.WIDTH, grid.HEIGHT);

        // compute new bulk u values
        for (int y = 1; y <= grid.HEIGHT; y++) {
            for (int x = 1; x <= grid.WIDTH; x++) {
                // do not compute for walls
                if (wallGrid.canFlowRight(x, y)) {
                    newBulk.getCell(x, y).ux = bulk.getCell(x, y).ux + BulkFlowComputer.computeUXDerivative(x, y, bulk) * Config.TIME_STEP;
                }
                if (wallGrid.canFlowDown(x, y)) {
                    newBulk.getCell(x, y).uy = bulk.getCell(x, y).uy + BulkFlowComputer.computeUYDerivative(x, y, bulk) * Config.TIME_STEP;
                }
            }
        }

        newBulk.clampU();

        // compute q bulk values
        for (int y = 1; y <= grid.HEIGHT; y++) {
            for (int x = 1; x <= grid.WIDTH; x++) {
                Cell cell = newBulk.getCell(x, y);

                Pair<Float, Float> upwindH = bulk.getUpwindH(x, y);

                // do not compute for walls
                if (wallGrid.canFlowRight(x, y))
                    cell.qx = cell.ux * upwindH.getLeft();
                if (wallGrid.canFlowDown(x, y))
                    cell.qy = cell.uy * upwindH.getRight();
            }
        }

        return newBulk;
    }
}
