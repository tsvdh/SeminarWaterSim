package org.watersim;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jtransforms.fft.FloatFFT_1D;

import java.nio.file.Path;
import java.util.Arrays;

public class Simulator {

    private Grid prevGrid;
    private Grid grid;

    private final WallGrid wallGrid;

    public Simulator(Path path) {
        Pair<Grid, WallGrid> input = Grid.parseInput(path);

        grid = input.getLeft();
        prevGrid = grid.copy();

        wallGrid = input.getRight();
    }

    public Grid getGrid() {
        return grid;
    }

    public Grid makeNewGrid() {
        // Pair<Grid, Grid> grids = Decomposer.decompose(grid, wallGrid);
        //
        // System.out.println(grids.getLeft());
        // System.out.println(grids.getRight());
        //
        // return null;

        // Grid prevBulk = Decomposer.decomposeBulkOnly(prevGrid).getLeft();
        // Grid bulk = Decomposer.decomposeBulkOnly(grid).getLeft();
        //
        // // compute u velocities
        // for (int y = 1; y <= grid.HEIGHT; y++) {
        //     for (int x = 1; x <= grid.WIDTH; x++) {
        //         Cell cell = bulk.getCell(x, y);
        //
        //         Pair<Float, Float> upwindH = prevBulk.getUpwindH(x, y);
        //
        //         // do not compute for walls
        //         if (wallGrid.canFlowRight(x, y))
        //             cell.ux = upwindH.getLeft() == 0 ? 0 : cell.qx / upwindH.getLeft();
        //         if (wallGrid.canFlowDown(x, y))
        //             cell.uy = upwindH.getRight() == 0 ? 0 : cell.qy / upwindH.getRight();
        //     }
        // }
        //
        // Grid newBulk = new Grid(grid.WIDTH, grid.HEIGHT);
        //
        // // compute new bulk values
        // for (int y = 1; y <= grid.HEIGHT; y++) {
        //     for (int x = 1; x <= grid.WIDTH; x++) {
        //         // do not compute for walls
        //         if (wallGrid.canFlowRight(x, y)) {
        //             float newU = bulk.getCell(x, y).ux + BulkFlowComputer.computeUXDerivative(x, y, bulk) * Config.TIME_STEP;
        //             newBulk.getCell(x, y).ux = clampU(newU);
        //         }
        //         if (wallGrid.canFlowDown(x, y)) {
        //             float newU = bulk.getCell(x, y).uy + BulkFlowComputer.computeUYDerivative(x, y, bulk) * Config.TIME_STEP;
        //             newBulk.getCell(x, y).uy = clampU(newU);
        //         }
        //     }
        // }
        //
        // // compute q velocities
        // for (int y = 1; y <= grid.HEIGHT; y++) {
        //     for (int x = 1; x <= grid.WIDTH; x++) {
        //         Cell cell = newBulk.getCell(x, y);
        //
        //         Pair<Float, Float> upwindH = bulk.getUpwindH(x, y);
        //
        //         // do not compute for walls
        //         if (wallGrid.canFlowRight(x, y))
        //             cell.qx = cell.ux * upwindH.getLeft();
        //         if (wallGrid.canFlowDown(x, y))
        //             cell.qy = cell.uy * upwindH.getRight();
        //     }
        // }
        //
        // // update heights with flow divergence
        // for (int y = 1; y <= grid.HEIGHT; y++) {
        //     for (int x = 1; x <= grid.WIDTH; x++) {
        //         // TODO: perhaps only for non walls
        //         Cell cell = newBulk.getCell(x, y);
        //         Cell leftCell = newBulk.getCell(x - 1, y);
        //         Cell upCell = newBulk.getCell(x, y - 1);
        //
        //         float divergence = (cell.qx - leftCell.qx) / Config.CELL_SIZE + (cell.qy - upCell.qy) / Config.CELL_SIZE;
        //         divergence *= -1;
        //
        //         cell.h = bulk.getCell(x, y).h + divergence * Config.TIME_STEP;
        //     }
        // }
        //
        // prevGrid = bulk;
        // grid = newBulk;
        //
        // return newBulk;

        Grid prevSurface = Decomposer.decomposeSurfaceOnly(prevGrid).getRight();
        Grid surface = Decomposer.decomposeSurfaceOnly(grid).getRight();

        Grid newSurface = AiryWaveComputer.computeSurfaceQ(grid, surface, prevSurface);

        // update heights with flow divergence
        for (int y = 1; y <= grid.HEIGHT; y++) {
            for (int x = 1; x <= grid.WIDTH; x++) {
                // TODO: perhaps only for non walls
                Cell cell = newSurface.getCell(x, y);
                Cell leftCell = newSurface.getCell(x - 1, y);
                Cell upCell = newSurface.getCell(x, y - 1);

                float divergence = (cell.qx - leftCell.qx) / Config.CELL_SIZE + (cell.qy - upCell.qy) / Config.CELL_SIZE;
                divergence *= -1;

                cell.h = surface.getCell(x, y).h + divergence * Config.TIME_STEP;
            }
        }

        prevGrid = surface;
        grid = newSurface;

        return newSurface;

        // int width = 8;
        // var fft = new FloatFFT_1D(width);
        // float[] data = new float[width * 2];
        //
        // periodic data
        // for (int i = 0; i < width; i++) {
        //     data[i] = (float) Math.sin(i * Math.PI / 4);
        //     if (Math.abs(data[i]) < 0.0001)
        //         data[i] = 0;
        // }
        //
        // System.out.println(Arrays.toString(data));
        //
        // fft.realForwardFull(data);
        //
        // // float[] unShifted = new float[width * 2];
        //
        // for (int k = 0; k < width; k++) {
        //     int realI = k * 2;
        //     int imI = realI + 1;
        //
        //     float multiplier = (float) (2 * Math.PI / width);
        //
        //     float half = width / 2f;
        //     if (k < half)
        //         multiplier *= k;
        //     else if (k > half)
        //         multiplier *= k - width;
        //     else
        //         multiplier = 0;
        //
        //     // multiplier *= half - Math.abs(half - k);
        //
        //     float realPart = data[realI];
        //     float imPart = data[imI];
        //
        //     data[realI] = -imPart * multiplier;
        //     data[imI] = realPart * multiplier;
        //
        //     // unShifted[realI] = data[realI];
        //     // unShifted[imI] = data[imI];
        //     //
        //     // float shiftTerm = -multiplier / 2f;
        //     // float shiftReal = (float) Math.cos(shiftTerm);
        //     // float shiftIm = (float) Math.sin(shiftTerm);
        //     //
        //     // realPart = data[realI];
        //     // imPart = data[imI];
        //     //
        //     // // (a + ib) * (c * id) = ac + iad + ibc - bd
        //     // data[realI] = realPart * shiftReal - imPart * shiftIm;
        //     // data[imI] = realPart * shiftIm + imPart * shiftReal;
        // }
        //
        // fft.complexInverse(data, true);
        // // fft.complexInverse(unShifted, true);
        //
        // // System.out.println(Arrays.toString(unShifted));
        // System.out.println(Arrays.toString(data));

        // data[2] = 1;
        // data[3] = 1;
        //
        // System.out.println(Arrays.toString(data));
        //
        // fft.realForwardFull(data);
        //
        // // float[] unShifted = new float[width * 2];
        //
        // for (int k = 0; k < width; k++) {
        //     int realI = k * 2;
        //     int imI = realI + 1;
        //
        //     float multiplier = (float) (2 * Math.PI / width);
        //
        //     float half = width / 2f;
        //     if (k < half)
        //         multiplier *= k;
        //     else if (k > half)
        //         multiplier *= k - width;
        //     else
        //         multiplier = 0;
        //
        //     // multiplier *= half - Math.abs(half - k);
        //
        //     float realPart = data[realI];
        //     float imPart = data[imI];
        //
        //     data[realI] = -imPart * multiplier;
        //     data[imI] = realPart * multiplier;
        //
        //     // unShifted[realI] = data[realI];
        //     // unShifted[imI] = data[imI];
        //     //
        //     // float shiftTerm = -multiplier / 2f;
        //     // float shiftReal = (float) Math.cos(shiftTerm);
        //     // float shiftIm = (float) Math.sin(shiftTerm);
        //     //
        //     // realPart = data[realI];
        //     // imPart = data[imI];
        //     //
        //     // // (a + ib) * (c * id) = ac + iad + ibc - bd
        //     // data[realI] = realPart * shiftReal - imPart * shiftIm;
        //     // data[imI] = realPart * shiftIm + imPart * shiftReal;
        // }
        //
        // fft.complexInverse(data, true);
        // // fft.complexInverse(unShifted, true);
        //
        // // System.out.println(Arrays.toString(unShifted));
        // System.out.println(Arrays.toString(data));
        //
        // return null;
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
