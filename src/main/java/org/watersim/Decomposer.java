package org.watersim;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import static org.watersim.Config.HEIGHT;
import static org.watersim.Config.WIDTH;

public class Decomposer {

    public static Pair<Grid, Grid> decomposeBulkOnly(Grid grid) {
        return new ImmutablePair<>(grid.copy(), new Grid(WIDTH, HEIGHT));
    }

    public static Pair<Grid, Grid> decomposeSurfaceOnly(Grid grid) {
        return new ImmutablePair<>(new Grid(WIDTH, HEIGHT), grid.copy());
    }

    public static Pair<Grid, Grid> decompose(Grid grid, WallGrid wallGrid) {
        var surface = new Grid(WIDTH, HEIGHT);
        var bulk = grid.copy();

        var diffusionCoefficients = new Grid(WIDTH, HEIGHT);

        for (int y = 0; y <= grid.WIDTH; y++) {
            for (int x = 0; x <= grid.HEIGHT; x++) {
                Cell curCell = grid.getCell(x, y);
                Cell rightCell = grid.getCell(x + 1, y);
                Cell downCell = grid.getCell(x, y + 1);
                Cell curCoefficient = diffusionCoefficients.getCell(x, y);

                boolean canFlowRight = wallGrid.canFlowRight(x, y);
                boolean canFlowDown = wallGrid.canFlowDown(x, y);

                if (canFlowRight)
                    curCoefficient.qx = getDiffusionCoefficient(curCell.h, rightCell.h);
                else if (curCell.h > 0)
                    curCoefficient.qx = getDiffusionCoefficient(curCell.h, curCell.h);
                else
                    curCoefficient.qx = getDiffusionCoefficient(rightCell.h, rightCell.h);

                if (canFlowDown)
                    curCoefficient.qy = getDiffusionCoefficient(curCell.h, downCell.h);
                else if (curCell.h > 0)
                    curCoefficient.qy = getDiffusionCoefficient(curCell.h, curCell.h);
                else
                    curCoefficient.qy = getDiffusionCoefficient(downCell.h, downCell.h);
            }
        }

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

                    boolean canFlowLeft = wallGrid.canFlowLeft(x, y);
                    boolean canFlowRight = wallGrid.canFlowRight(x, y);
                    boolean canFlowUp = wallGrid.canFlowUp(x, y);
                    boolean canFlowDown = wallGrid.canFlowDown(x, y);

                    @SuppressWarnings("DuplicatedCode")
                    Cell curCoefficient = diffusionCoefficients.getCell(x, y);
                    Cell leftCoefficient = diffusionCoefficients.getCell(x - 1, y);
                    Cell rightCoefficient = diffusionCoefficients.getCell(x + 1, y);
                    Cell upCoefficient = diffusionCoefficients.getCell(x, y - 1);
                    Cell downCoefficient = diffusionCoefficients.getCell(x, y + 1);

                    // 0: h, 1: qx, 2: qy
                    float[] leftDiff = new float[3];
                    float[] rightDiff = new float[3];
                    float[] upDiff = new float[3];
                    float[] downDiff = new float[3];

                    if (canFlowLeft) {
                        leftDiff[0] = (leftCell.h - curCell.h) * leftCoefficient.qx;
                        leftDiff[1] = (leftCell.qx - curCell.qx) * (leftCoefficient.qx + curCoefficient.qx) / 2;
                        // leftDiff[2] = (leftCell.qy - curCell.qy) * (leftCoefficient.qy + curCoefficient.qy) / 2;
                    }
                    if (canFlowRight) {
                        rightDiff[0] = (rightCell.h - curCell.h) * curCoefficient.qx;
                        rightDiff[1] = (rightCell.qx - curCell.qx) * (rightCoefficient.qx + curCoefficient.qx) / 2;
                        // rightDiff[2] = (rightCell.qy - curCell.qy) * (rightCoefficient.qy + curCoefficient.qy) / 2;
                    }
                    if (canFlowUp) {
                        upDiff[0] = (upCell.h - curCell.h) * upCoefficient.qy;
                        // upDiff[1] = (upCell.qx - curCell.qx) * (upCoefficient.qx + curCoefficient.qx) / 2;
                        upDiff[2] = (upCell.qy - curCell.qy) * (upCoefficient.qy + curCoefficient.qy) / 2;
                    }
                    if (canFlowDown) {
                        downDiff[0] = (downCell.h - curCell.h) * curCoefficient.qy;
                        // downDiff[1] = (downCell.qx - curCell.qx) * (downCoefficient.qx + curCoefficient.qx) / 2;
                        downDiff[2] = (downCell.qy - curCell.qy) * (downCoefficient.qy + curCoefficient.qy) / 2;
                    }

                    float cellSizeSquared = (float) Math.pow(Config.CELL_SIZE, 2);
                    float timeStep = 0.25f;

                    float[] changes = new float[3];
                    for (int j = 0; j < 3; j++) {
                        changes[j] = timeStep * ((leftDiff[j] + rightDiff[j] + upDiff[j] + downDiff[j]) / cellSizeSquared);
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

    private static float getDiffusionCoefficient(float curH, float otherH) {
        float averageH = (curH + otherH) / 2;
        float gradient = otherH - curH;

        float diffusionCoefficient = (float) ((Math.pow(averageH, 2) / 64) * Math.exp(-1f / 100 * Math.pow(gradient, 2)));
        diffusionCoefficient = Math.min(diffusionCoefficient, 1);

        return diffusionCoefficient;
    }
}
