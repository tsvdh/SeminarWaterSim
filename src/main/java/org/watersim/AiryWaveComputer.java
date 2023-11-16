package org.watersim;

import org.jtransforms.fft.FloatFFT_2D;
import java.util.Arrays;

public class AiryWaveComputer {

    public static Grid computeSurfaceQ(Grid grid, Grid surface, Grid prevSurface) {
        if (grid.WIDTH != grid.HEIGHT)
            throw new RuntimeException();

        // set up arrays
        float[][] surfaceH = new float[grid.HEIGHT][grid.WIDTH * 2];
        float[][] surfaceQX = new float[grid.HEIGHT][grid.WIDTH * 2];
        float[][] surfaceQY = new float[grid.HEIGHT][grid.WIDTH * 2];
        float[][][] newSurfaceQX = new float[4][grid.HEIGHT][grid.WIDTH * 2];
        float[][][] newSurfaceQY = new float[4][grid.HEIGHT][grid.WIDTH * 2];

        for (int y = 1; y <= grid.HEIGHT; y++) {
            for (int x = 1; x <= grid.WIDTH; x++) {
                surfaceH[y - 1][x - 1] = (surface.getCell(x, y).h + prevSurface.getCell(x, y).h) / 2;
                surfaceQX[y - 1][x - 1] = surface.getCell(x, y).qx;
                surfaceQY[y - 1][x - 1] = surface.getCell(x, y).qy;
            }
        }

        // compute heights for interpolation
        float[] heights = new float[4];
        for (int i = 0; i < 4; i++) {
            heights[i] = (float) Math.pow(2, i * 2);
        }

        // compute fft arrays
        var fft = new FloatFFT_2D(grid.WIDTH, grid.HEIGHT);
        fft.realForwardFull(surfaceH);
        fft.realForwardFull(surfaceQX);
        fft.realForwardFull(surfaceQY);

        float half = grid.WIDTH / 2f;

        for (int x = 0; x < grid.WIDTH; x++) {
            int realX = x * 2;
            int imX = realX + 1;

            for (int y = 0; y < grid.HEIGHT; y++) {
                float kX = half - Math.abs(half - x); // square grid, halfway works for both dimensions
                float kY = half - Math.abs(half - y);

                // float kX = x < half ? x
                //         : x > half ? x - grid.WIDTH
                //         : 0;
                // float kY = y < half ? y
                //         : y > half ? y - grid.HEIGHT
                //         : 0;

                float k = (float) (2 * Math.PI * Math.sqrt(Math.pow(kX, 2) + Math.pow(kY, 2)) / grid.WIDTH);

                kX = (float) (2 * Math.PI * Math.abs(kX) / grid.WIDTH);
                kY = (float) (2 * Math.PI * Math.abs(kY) / grid.HEIGHT);

                // compute derivative
                float realDX = -kX * surfaceH[y][imX];
                float imDX = kX * surfaceH[y][realX];
                float realDY = -kY * surfaceH[y][imX];
                float imDY = kY * surfaceH[y][realX];

                // shift h to q position
                float shiftTermX = kX * Config.CELL_SIZE / 2;
                float shiftTermY = kY * Config.CELL_SIZE / 2;

                float shiftRealX = (float) Math.cos(shiftTermX);
                float shiftImX = (float) Math.sin(shiftTermX);
                float shiftRealY = (float) Math.cos(shiftTermY);
                float shiftImY = (float) Math.sin(shiftTermY);

                // (a + ib) * (c * id) = ac + iad + ibc - bd
                float realShiftedDX = realDX * shiftRealX - imDX * shiftImX;
                float imShiftedDX = realDX * shiftImX + imDX * shiftRealX;
                float realShiftedDY = realDY * shiftRealY - imDY * shiftImY;
                float imShiftedDY = realDY * shiftImY + imDY * shiftRealY;

                for (int i = 0; i < 4; i++) {
                    // float omegaX = (float) Math.sqrt(Config.GRAVITY * kX * Math.tanh(kX * heights[i]));
                    // float omegaY = (float) Math.sqrt(Config.GRAVITY * kY * Math.tanh(kY * heights[i]));
                    //
                    // // compute new q
                    // float qPartX = (float) Math.cos(omegaX * Config.TIME_STEP);
                    // float qPartY = (float) Math.cos(omegaY * Config.TIME_STEP);
                    // float hPartX = omegaX == 0 ? 0 : (float) (Math.sin(omegaX * Config.TIME_STEP) * omegaX / Math.pow(kX, 2));
                    // float hPartY = omegaY == 0 ? 0 : (float) (Math.sin(omegaY * Config.TIME_STEP) * omegaY / Math.pow(kY, 2));
                    //
                    // newSurfaceQX[i][y][realX] = qPartX * surfaceQX[y][realX] - hPartX * realShiftedDX;
                    // newSurfaceQX[i][y][imX] = qPartX * surfaceQX[y][imX] - hPartX * imShiftedDX;
                    // newSurfaceQY[i][y][realX] = qPartY * surfaceQY[y][realX] - hPartY * realShiftedDY;
                    // newSurfaceQY[i][y][imX] = qPartY * surfaceQY[y][imX] - hPartY * imShiftedDY;

                    float omega = (float) Math.sqrt(Config.GRAVITY * k * Math.tanh(k * heights[i]));

                    float qPart = (float) Math.cos(omega * Config.TIME_STEP);
                    float hPart = k == 0 ? 0 : (float) (Math.sin(omega * Config.TIME_STEP) * omega / Math.pow(k, 2));
                    // float hPart = k == 0 ? 0 : (float) (Math.sin(omega * Config.TIME_STEP) * Config.GRAVITY / omega);

                    newSurfaceQX[i][y][realX] = qPart * surfaceQX[y][realX] - hPart * realShiftedDX;
                    newSurfaceQX[i][y][imX] = qPart * surfaceQX[y][imX] - hPart * imShiftedDX;
                    newSurfaceQY[i][y][realX] = qPart * surfaceQY[y][realX] - hPart * realShiftedDY;
                    newSurfaceQY[i][y][imX] = qPart * surfaceQY[y][imX] - hPart * imShiftedDY;
                }
            }
        }

        // invert fft for all heights
        for (int i = 0; i < 4; i++) {
            fft.complexInverse(newSurfaceQX[i], true);
            fft.complexInverse(newSurfaceQY[i], true);
        }

        // for (int y = 0; y < grid.HEIGHT; y++) {
        //     System.out.println(Arrays.toString(newSurfaceQX[0][y]));
        // }
        // System.out.println();
        // for (int y = 0; y < grid.HEIGHT; y++) {
        //     System.out.println(Arrays.toString(newSurfaceQY[0][y]));
        // }
        //
        // System.exit(0);

        var newSurface = new Grid(grid.WIDTH, grid.HEIGHT);

        // interpolate to correct height
        for (int y = 1; y <= grid.HEIGHT; y++) {
            int yIndex = y - 1;

            for (int x = 1; x <= grid.WIDTH; x++) {
                int fftIndex = (x - 1) * 2;

                int heightBelow = 0;
                int heightAbove = 3;

                float curH = grid.getCell(x, 1).h;

                // tighten bounds
                for (int i = 0; i < 4; i++) {
                    if (heights[i] <= curH)
                        heightBelow = i;
                }
                for (int i = 3; i >= 0; i--) {
                    if (heights[i] > curH)
                        heightAbove = i;
                }

                Cell cell = newSurface.getCell(x, y);

                float w = heightBelow == heightAbove ? 0
                        : (curH - heights[heightBelow]) / (heights[heightAbove] - heights[heightBelow]);

                float qXBelow = newSurfaceQX[heightBelow][yIndex][fftIndex];
                float qXAbove = newSurfaceQX[heightAbove][yIndex][fftIndex];
                cell.qx = qXBelow * (1 - w) + qXAbove * w;

                float qYBelow = newSurfaceQY[heightBelow][yIndex][fftIndex];
                float qYAbove = newSurfaceQY[heightAbove][yIndex][fftIndex];
                cell.qy = qYBelow * (1 - w) + qYAbove * w;
            }
        }

        return newSurface;
    }
}

// TODO: delete if not needed
// float k = (float) Math.sqrt(Math.pow(surfaceH[realX], 2) + Math.pow(surfaceH[imX], 2));
//
// // differentiate h
// float derivativeMultiplier = k; // (2pi / L), L = (2pi / k), => k
// float hReal = surfaceH[realX];
// float hIm = surfaceH[imX];
//
// if (x < halfWay) {
//     derivativeMultiplier *= x;
//     derivativeH[realX] = -hIm * derivativeMultiplier;
//     derivativeH[imX] = hReal * derivativeMultiplier;
// }
// else if (x > halfWay) {
//     derivativeMultiplier *= (x - grid.WIDTH);
//     derivativeH[realX] = -hIm * derivativeMultiplier;
//     derivativeH[imX] = hReal * derivativeMultiplier;
// }
// else {
//     derivativeH[realX] = 0;
//     derivativeH[imX] = 0;
// }