package org.watersim;

import org.apache.commons.lang3.tuple.Pair;

public class BulkFlowComputer {

    public static float computeHDerivative(int x, int y, Grid bulk) {
        Cell curCell = bulk.getCell(x, y);
        Cell leftCell = bulk.getCell(x - 1, y);
        Cell upCell = bulk.getCell(x, y - 1);

        Pair<Float, Float> upwindH = bulk.getUpwindH(x, y);

        float xPart = ((upwindH.getLeft() * curCell.ux) - (bulk.getUpwindH(x - 1, y).getLeft() * leftCell.ux)) / Config.CELL_SIZE;
        float yPart = ((upwindH.getRight() * curCell.uy) - (bulk.getUpwindH(x, y - 1).getRight() * upCell.uy)) / Config.CELL_SIZE;

        return -1 * (xPart + yPart);
    }

    @SuppressWarnings("DuplicatedCode")
    public static float computeUXDerivative(int x, int y, Grid bulk) {
        Cell curCell = bulk.getCell(x, y);
        Cell leftCell = bulk.getCell(x - 1, y);
        Cell rightCell = bulk.getCell(x + 1, y);
        Cell upCell = bulk.getCell(x, y - 1);
        Cell downCell = bulk.getCell(x, y + 1);

        Pair<Float, Float> upwindH = bulk.getUpwindH(x, y);

        float firstPart = 0;
        if (upwindH.getLeft() != 0) {
            float goingRight = (bulk.getAverageQ(x, y).getLeft() / upwindH.getLeft()) * ((curCell.ux - leftCell.ux) / Config.CELL_SIZE);
            float goingLeft = (bulk.getAverageQ(x + 1, y).getLeft() / upwindH.getLeft()) * ((rightCell.ux - curCell.ux) / Config.CELL_SIZE);

            if (curCell.ux > 0) {
                firstPart = goingRight;
            }
            else if (curCell.ux < 0) {
                firstPart = goingLeft;
            }
            else {
                firstPart = goingRight + goingLeft;
            }
        }

        float secondPart = 0;
        if (upwindH.getLeft() != 0) {
            if (upCell.uy > 0) {
                secondPart += (upCell.qy / upwindH.getLeft()) * ((curCell.ux - upCell.ux) / Config.CELL_SIZE);
            }
            if (curCell.uy < 0) {
                secondPart += (-curCell.qy / upwindH.getLeft()) * ((curCell.ux - downCell.ux) / Config.CELL_SIZE);
            }
        }

        float thirdPart = Config.GRAVITY * (bulk.getCell(x + 1, y).h - curCell.h) / Config.CELL_SIZE;

        return -1 * (firstPart + secondPart + thirdPart);
    }

    @SuppressWarnings("DuplicatedCode")
    public static float computeUYDerivative(int x, int y, Grid bulk) {
        Cell curCell = bulk.getCell(x, y);
        Cell leftCell = bulk.getCell(x - 1, y);
        Cell rightCell = bulk.getCell(x + 1, y);
        Cell upCell = bulk.getCell(x, y - 1);
        Cell downCell = bulk.getCell(x, y + 1);

        Pair<Float, Float> upwindH = bulk.getUpwindH(x, y);

        float firstPart = 0;
        if (upwindH.getRight() != 0) {
            if (leftCell.ux > 0) {
                firstPart += (leftCell.qx / upwindH.getRight()) * ((curCell.uy - leftCell.uy) / Config.CELL_SIZE);
            }
            if (curCell.ux < 0) {
                firstPart += (-curCell.qx / upwindH.getRight()) * ((curCell.uy - rightCell.uy) / Config.CELL_SIZE);
            }
        }

        float secondPart = 0;
        if (upwindH.getRight() != 0) {
            float goingDown = (bulk.getAverageQ(x, y).getRight() / upwindH.getRight()) * ((curCell.uy - upCell.uy) / Config.CELL_SIZE);
            float goingUp = (bulk.getAverageQ(x, y + 1).getRight() / upwindH.getRight()) * ((downCell.uy - curCell.uy) / Config.CELL_SIZE);

            if (curCell.uy > 0) {
               secondPart = goingDown;
            }
            else if (curCell.uy < 0) {
               secondPart = goingUp;
            }
            else {
                secondPart = goingDown + goingUp;
            }
        }

        float thirdPart = Config.GRAVITY * (bulk.getCell(x, y + 1).h - curCell.h) / Config.CELL_SIZE;

        return -1 * (firstPart + secondPart + thirdPart);
    }
}
