package org.watersim;

import org.apache.commons.lang3.tuple.Pair;

public class BulkFlowComputer {

    public static float computeHDerivative(int x, int y, Grid bulk) {
        Cell curCell = bulk.getCell(x, y);
        Cell leftCell = bulk.getCell(x - 1, y);
        Cell upCell = bulk.getCell(x, y - 1);

        Pair<Float, Float> averageH = bulk.getAverageH(x, y);

        float xPart = ((averageH.getLeft() * curCell.ux) - (bulk.getAverageH(x - 1, y).getLeft() * leftCell.ux)) / Config.CELL_SIZE;
        float yPart = ((averageH.getRight() * curCell.uy) - (bulk.getAverageH(x, y - 1).getRight() * upCell.uy)) / Config.CELL_SIZE;

        return -1 * (xPart + yPart);
    }

    public static float computeUXDerivative(int x, int y, Grid bulk) {
        Cell curCell = bulk.getCell(x, y);
        Cell leftCell = bulk.getCell(x - 1, y);
        Cell upCell = bulk.getCell(x, y - 1);

        Pair<Float, Float> averageH = bulk.getAverageH(x, y);

        float firstPart = (bulk.getAverageQ(x, y).getLeft() / averageH.getLeft()) * ((curCell.ux - leftCell.ux) / Config.CELL_SIZE);
        float secondPart = (upCell.qy / averageH.getLeft()) * ((curCell.ux - upCell.ux) / Config.CELL_SIZE);
        float thirdPart = Config.GRAVITY * (bulk.getCell(x + 1, y).h - curCell.h) / Config.CELL_SIZE;

        return -1 * (firstPart + secondPart + thirdPart);
    }

    public static float computeUYDerivative(int x, int y, Grid bulk) {
        Cell curCell = bulk.getCell(x, y);
        Cell leftCell = bulk.getCell(x - 1, y);
        Cell upCell = bulk.getCell(x, y - 1);

        Pair<Float, Float> averageH = bulk.getAverageH(x, y);

        float firstPart = (leftCell.qx / averageH.getRight()) * ((curCell.uy - leftCell.uy) / Config.CELL_SIZE);
        float secondPart = (bulk.getAverageQ(x, y).getRight() / averageH.getRight()) * ((curCell.uy - upCell.uy) / Config.CELL_SIZE);
        float thirdPart = Config.GRAVITY * (bulk.getCell(x, y + 1).h - curCell.h) / Config.CELL_SIZE;

        return -1 * (firstPart + secondPart + thirdPart);
    }
}
