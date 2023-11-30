package org.watersim;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class Decomposer {

    public static Pair<Grid, Grid> decomposeBulkOnly(Grid grid) {
        return new ImmutablePair<>(grid.copy(), new Grid(grid.WIDTH, grid.HEIGHT));
    }

    public static Pair<Grid, Grid> decomposeSurfaceOnly(Grid grid) {
        return new ImmutablePair<>(new Grid(grid.WIDTH, grid.HEIGHT), grid.copy());
    }

    public static Pair<Grid, Grid> decompose(Grid grid, WallGrid wallGrid) {
        var surface = new Grid(grid.WIDTH, grid.HEIGHT);
        var bulk = grid.copy();

        for (int i = 0; i < 128; i++) {
            var newBulk = bulk.copy();

            for (int y = 1; y <= grid.WIDTH; y++) {
                for (int x = 1; x <= grid.HEIGHT; x++) {
                    if (grid.getCell(x, y).h == 0 || wallGrid.getCell(x, y).h < 0)
                        continue;

                    @SuppressWarnings("DuplicatedCode")
                    Cell curCell = bulk.getCell(x, y);
                    Cell leftCell = bulk.getCell(x - 1, y);
                    Cell rightCell = bulk.getCell(x + 1, y);
                    Cell upCell = bulk.getCell(x, y - 1);
                    Cell downCell = bulk.getCell(x, y + 1);

                    // 0: h, 1: qx, 2: qy
                    float[] leftDiff = new float[3];
                    float[] rightDiff = new float[3];
                    float[] upDiff = new float[3];
                    float[] downDiff = new float[3];

                    if (wallGrid.canFlowLeft(x, y)) {
                        leftDiff[0] = leftCell.h - curCell.h;
                        leftDiff[1] = leftCell.qx - curCell.qx;
                        leftDiff[2] = leftCell.qy - curCell.qy;
                    }
                    if (wallGrid.canFlowRight(x, y)) {
                        rightDiff[0] = rightCell.h - curCell.h;
                        rightDiff[1] = rightCell.qx - curCell.qx;
                        rightDiff[2] = rightCell.qy - curCell.qy;
                    }
                    if (wallGrid.canFlowUp(x, y)) {
                        upDiff[0] = upCell.h - curCell.h;
                        upDiff[1] = upCell.qx - curCell.qx;
                        upDiff[2] = upCell.qy - curCell.qy;
                    }
                    if (wallGrid.canFlowDown(x, y)) {
                        downDiff[0] = downCell.h - curCell.h;
                        downDiff[1] = downCell.qx - curCell.qx;
                        downDiff[2] = downCell.qy - curCell.qy;
                    }

                    float horizontalGradient = 0;
                    float verticalGradient = 0;

                    if (wallGrid.canFlowLeft(x, y))
                        horizontalGradient -= leftCell.h;
                    else horizontalGradient -= curCell.h;

                    if (wallGrid.canFlowRight(x, y))
                        horizontalGradient += rightCell.h;
                    else horizontalGradient += curCell.h;

                    if (wallGrid.canFlowUp(x, y))
                        verticalGradient -= upCell.h;
                    else verticalGradient -= curCell.h;

                    if (wallGrid.canFlowDown(x, y))
                        verticalGradient += downCell.h;
                    else verticalGradient += curCell.h;

                    float gradientLength = (float) Math.sqrt(Math.pow(horizontalGradient, 2) + Math.pow(verticalGradient, 2));
                    float cellSizeSquared = (float) Math.pow(Config.CELL_SIZE, 2);
                    float diffusionCoefficient = (float) ((Math.pow(grid.getCell(x, y).h, 2) / 64) * Math.exp(-1f / 100 * Math.pow(gradientLength, 2)));
                    float timeStep = 0.25f;

                    float[] changes = new float[3];
                    for (int j = 0; j < 3; j++) {
                        changes[j] = diffusionCoefficient * timeStep
                                * (((leftDiff[j] + rightDiff[j]) / cellSizeSquared) + ((upDiff[j] + downDiff[j]) / cellSizeSquared));
                    }

                    Cell newCell = newBulk.getCell(x, y);
                    newCell.h += changes[0];
                    newCell.qx += changes[1];
                    newCell.qy += changes[2];
                }
            }

            bulk = newBulk;
        }

        for (int x = 1; x <= grid.WIDTH; x++) {
            for (int y = 1; y <= grid.HEIGHT; y++) {
                surface.getCell(x, y).h = grid.getCell(x, y).h - bulk.getCell(x, y).h;
                surface.getCell(x, y).qx = grid.getCell(x, y).qx - bulk.getCell(x, y).qx;
                surface.getCell(x, y).qy = grid.getCell(x, y).qy - bulk.getCell(x, y).qy;
            }
        }

        return new ImmutablePair<>(bulk, surface);
    }
}
