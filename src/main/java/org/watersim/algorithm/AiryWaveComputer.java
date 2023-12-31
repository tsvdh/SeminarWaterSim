package org.watersim.algorithm;

import org.jtransforms.fft.FloatFFT_2D;
import org.watersim.grid.Cell;
import org.watersim.grid.WallGrid;
import org.watersim.util.Config;
import org.watersim.grid.Grid;

import static org.watersim.util.Config.HEIGHT;
import static org.watersim.util.Config.WIDTH;
import static org.watersim.util.Utils.lerp;

public class AiryWaveComputer {

    public static Grid computeSurfaceQ(Grid grid, Grid surface, Grid prevSurface, WallGrid wallGrid) {
        // set up arrays
        float[][] surfaceH = new float[HEIGHT][WIDTH * 2];
        float[][] surfaceQX = new float[HEIGHT][WIDTH * 2];
        float[][] surfaceQY = new float[HEIGHT][WIDTH * 2];
        float[][][] newSurfaceQX = new float[4][HEIGHT][WIDTH * 2];
        float[][][] newSurfaceQY = new float[4][HEIGHT][WIDTH * 2];

        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
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
        var fft = new FloatFFT_2D(WIDTH, HEIGHT);
        fft.realForwardFull(surfaceH);
        fft.realForwardFull(surfaceQX);
        fft.realForwardFull(surfaceQY);

        float half = WIDTH / 2f;

        for (int x = 0; x < WIDTH; x++) {
            int realX = x * 2;
            int imX = realX + 1;

            for (int y = 0; y < HEIGHT; y++) {
                float kX = x < half ? x
                        : x > half ? x - WIDTH
                        : 0;
                float kY = y < half ? y
                        : y > half ? y - HEIGHT
                        : 0;

                kX = (float) (2 * Math.PI * kX / WIDTH);
                kY = (float) (2 * Math.PI * kY / HEIGHT);

                float k = (float) Math.sqrt(Math.pow(kX, 2) + Math.pow(kY, 2));

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
                    float beta = (float) (Math.sqrt(2 / (k * Config.CELL_SIZE) * Math.sin(k * Config.CELL_SIZE / 2)));
                    float omega = (float) Math.sqrt(Config.GRAVITY * k * Math.tanh(k * heights[i])) / beta;

                    float qPart = k == 0 ? 0 : (float) Math.cos(omega * Config.TIME_STEP);
                    float hPart = k == 0 ? 0 : (float) (Math.sin(omega * Config.TIME_STEP) * omega / Math.pow(k, 2));

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

        var newSurface = new Grid();

        // interpolate to correct height
        for (int y = 1; y <= HEIGHT; y++) {
            int yIndex = y - 1;

            for (int x = 1; x <= WIDTH; x++) {
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
                cell.qx = lerp(qXBelow, qXAbove, w);

                float qYBelow = newSurfaceQY[heightBelow][yIndex][fftIndex];
                float qYAbove = newSurfaceQY[heightAbove][yIndex][fftIndex];
                cell.qy = lerp(qYBelow, qYAbove, w);
            }
        }

        return newSurface;
    }
}