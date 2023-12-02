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

        for (int y = 1; y <= grid.WIDTH; y++) {
            for (int x = 1; x <= grid.HEIGHT; x++) {
                @SuppressWarnings("DuplicatedCode")
                Cell curCell = grid.getCell(x, y);
                Cell leftCell = grid.getCell(x - 1, y);
                Cell rightCell = grid.getCell(x + 1, y);
                Cell upCell = grid.getCell(x, y - 1);
                Cell downCell = grid.getCell(x, y + 1);

                boolean canFlowLeft = wallGrid.canFlowLeft(x, y);
                boolean canFlowRight = wallGrid.canFlowRight(x, y);
                boolean canFlowUp = wallGrid.canFlowUp(x, y);
                boolean canFlowDown = wallGrid.canFlowDown(x, y);

                float horizontalGradient = 0;
                float verticalGradient = 0;

                if (canFlowLeft)
                    horizontalGradient -= leftCell.h;
                else horizontalGradient -= curCell.h;

                if (canFlowRight)
                    horizontalGradient += rightCell.h;
                else horizontalGradient += curCell.h;

                if (canFlowUp)
                    verticalGradient -= upCell.h;
                else verticalGradient -= curCell.h;

                if (canFlowDown)
                    verticalGradient += downCell.h;
                else verticalGradient += curCell.h;

                if (canFlowLeft && canFlowRight)
                    horizontalGradient /= 2;

                if (canFlowUp && canFlowDown)
                    verticalGradient /= 2;

                float gradientLength = (float) Math.sqrt(Math.pow(horizontalGradient, 2) + Math.pow(verticalGradient, 2));
                float diffusionCoefficient = (float) ((Math.pow(grid.getCell(x, y).h, 2) / 64) * Math.exp(-1f / 100 * Math.pow(gradientLength, 2)));
                diffusionCoefficient = Math.min(diffusionCoefficient, 1);

                diffusionCoefficients.getCell(x, y).h = diffusionCoefficient;
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

                    // 0: h, 1: qx, 2: qy
                    float[] leftDiff = new float[3];
                    float[] rightDiff = new float[3];
                    float[] upDiff = new float[3];
                    float[] downDiff = new float[3];

                    if (canFlowLeft) {
                        leftDiff = cellDiff(curCell, leftCell);
                    }
                    if (canFlowRight) {
                        rightDiff = cellDiff(curCell, rightCell);
                    }
                    if (canFlowUp) {
                        upDiff = cellDiff(curCell, upCell);
                    }
                    if (canFlowDown) {
                        downDiff = cellDiff(curCell, downCell);
                    }

                    float cellSizeSquared = (float) Math.pow(Config.CELL_SIZE, 2);
                    float diffusionCoefficient = diffusionCoefficients.getCell(x, y).h;
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

    private static float[] cellDiff(Cell curCell, Cell otherCell) {
        float[] diff = new float[3];

        // 0: h, 1: qx, 2: qy
        diff[0] = otherCell.h - curCell.h;
        diff[1] = otherCell.qx - curCell.qx;
        diff[2] = otherCell.qy - curCell.qy;

        return diff;
    }
}
