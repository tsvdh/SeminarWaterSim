package org.watersim;

import org.apache.commons.lang3.tuple.ImmutablePair;

import static org.watersim.Config.HEIGHT;
import static org.watersim.Config.WIDTH;

public class SurfaceTransporter {

    private static final float GAMMA = 1f / 4;

    public static Grid transportSurface(Grid surface, Grid newSurface, Grid bulk, Grid newBulk) {
        Grid averageBulk = new Grid(WIDTH, HEIGHT);

        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
                Cell averageCell = averageBulk.getCell(x, y);
                Cell bulkCell = bulk.getCell(x, y);
                Cell newBulkCell = newBulk.getCell(x, y);

                averageCell.ux = (bulkCell.ux + newBulkCell.ux) / 2;
                averageCell.uy = (bulkCell.uy + newBulkCell.uy) / 2;
            }
        }

        averageBulk.computeDivergence();
        newBulk.computeDivergence();

        Grid dampedSurface = new Grid(WIDTH, HEIGHT);

        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
                Cell surfaceCell = surface.getCell(x, y);
                Cell newSurfaceCell = newSurface.getCell(x, y);
                Cell averageBulkCell = averageBulk.getCell(x, y);
                Cell newBulkCell = bulk.getCell(x, y);
                Cell dampedCell = dampedSurface.getCell(x, y);

                float qG = Math.min(-averageBulkCell.divU, GAMMA * -averageBulkCell.divU);
                float hG = Math.min(-newBulkCell.divU, GAMMA * -newBulkCell.divU);

                dampedCell.qx = newSurfaceCell.qx * (float) Math.exp(qG * Config.TIME_STEP);
                dampedCell.qy = newSurfaceCell.qy * (float) Math.exp(qG * Config.TIME_STEP);
                dampedCell.h = surfaceCell.h * (float) Math.exp(hG * Config.TIME_STEP);
            }
        }

        Grid advectedSurface = new Grid(WIDTH, HEIGHT);

        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
                Cell uCurcell = newBulk.getCell(x, y);
                Cell uLeftCell = newBulk.getCell(x, y);
                Cell uTopCell = newBulk.getCell(x, y);

                // --- Semi-Lagrangian advection on Q ---

                // Trace back to starting position
                float xPosQ = (float) Math.clamp(x + 0.5 - (uCurcell.ux * Config.TIME_STEP), 0.5, WIDTH + 0.5);
                float yPosQ = (float) Math.clamp(y + 0.5 - (uCurcell.uy * Config.TIME_STEP), 0.5, HEIGHT + 0.5);

                // TODO: consider walls

                int leftPos = (int) Math.max(Math.round(Math.floor(xPosQ - 0.5)), 1);
                int rightPos = (int) Math.min(Math.round(Math.ceil(xPosQ - 0.5)), WIDTH);
                int upPos = (int) Math.max(Math.round(Math.floor(yPosQ - 0.5)), 1);
                int downPos = (int) Math.min(Math.round(Math.ceil(yPosQ - 0.5)), HEIGHT);

                Cell leftCell = dampedSurface.getCell(leftPos, y);
                Cell rightCell = dampedSurface.getCell(rightPos, y);
                Cell upCell = dampedSurface.getCell(x, upPos);
                Cell downCell = dampedSurface.getCell(x, downPos);

                float horizontalWeight = xPosQ - (leftPos + 0.5f);
                float verticalWeight = yPosQ - (upPos + 0.5f);

                Cell advectedCell = advectedSurface.getCell(x, y);
                advectedCell.qx = lerp(leftCell.qx, rightCell.qx, horizontalWeight);
                advectedCell.qy = lerp(upCell.qy, downCell.qy, verticalWeight);

                // --- Semi-Lagrangian advection on H ---

                float horizontalU = (uCurcell.ux + uLeftCell.ux) / 2;
                float verticalU = (uCurcell.uy + uTopCell.uy) / 2;

                // Trace back to starting position
                // Transport forward in time only half a time step
                float xPosH = (float) Math.clamp(x - (horizontalU * Config.TIME_STEP / 2), 0.5, WIDTH + 0.5);
                float yPosH = (float) Math.clamp(y - (verticalU * Config.TIME_STEP / 2), 0.5, HEIGHT + 0.5);

                // TODO: consider walls

                int xMin = (int) Math.max(Math.round(Math.floor(xPosH)), 1);
                int xMax = (int) Math.min(Math.round(Math.ceil(xPosH)), WIDTH);
                int yMin = (int) Math.max(Math.round(Math.floor(yPosH)), 1);
                int yMax = (int) Math.min(Math.round(Math.ceil(yPosH)), HEIGHT);

                float topLeftVal = dampedSurface.getCell(xMin, yMin).h;
                float topRightVal = dampedSurface.getCell(xMax, yMin).h;
                float bottomLeftVal = dampedSurface.getCell(xMin, yMax).h;
                float bottomRightVal = dampedSurface.getCell(xMax, yMax).h;

                float lerpYMin = lerp(topLeftVal, topRightVal, xPosH - xMin);
                float lerpYMax = lerp(bottomLeftVal, bottomRightVal, xPosH - xMin);

                advectedCell.h = lerp(lerpYMin, lerpYMax, yPosH - yMin);
            }
        }

        advectedSurface.clampQ(new ImmutablePair<>(advectedSurface, advectedSurface));

        return advectedSurface;
    }

    private static float lerp(float a, float b, float w) {
        return a * (1 - w) + b * w;
    }
}
