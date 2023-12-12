package org.watersim.algorithm;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.watersim.grid.Cell;
import org.watersim.grid.WallGrid;
import org.watersim.util.Config;
import org.watersim.grid.Grid;

import static org.watersim.util.Config.HEIGHT;
import static org.watersim.util.Config.WIDTH;
import static org.watersim.util.Utils.lerp;

public class SurfaceTransporter {

    private static final float GAMMA = 1f / 4;

    public static Grid transportSurface(Grid surface, Grid newSurface, Grid bulk, Grid newBulk, WallGrid wallGrid) {
        Grid averageBulk = new Grid();

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

        Grid dampedSurface = new Grid();

        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
                Cell surfaceCell = surface.getCell(x, y);
                Cell newSurfaceCell = newSurface.getCell(x, y);
                Cell curAverageBulkCell = averageBulk.getCell(x, y);
                Cell rightAverageBulkCell = averageBulk.getCell(x + 1, y);
                Cell downAverageBulkCell = averageBulk.getCell(x, y + 1);
                Cell newBulkCell = bulk.getCell(x, y);
                Cell dampedCell = dampedSurface.getCell(x, y);

                float divUX = (curAverageBulkCell.divU + rightAverageBulkCell.divU) / 2;
                float divUY = (curAverageBulkCell.divU + downAverageBulkCell.divU) / 2;

                float qGX = Math.min(-divUX, GAMMA * -divUX);
                float qGY = Math.min(-divUY, GAMMA * -divUY);
                float hG = Math.min(-newBulkCell.divU, GAMMA * -newBulkCell.divU);

                dampedCell.qx = newSurfaceCell.qx * (float) Math.exp(qGX * Config.TIME_STEP);
                dampedCell.qy = newSurfaceCell.qy * (float) Math.exp(qGY * Config.TIME_STEP);
                dampedCell.h = surfaceCell.h * (float) Math.exp(hG * Config.TIME_STEP);
            }
        }

        Grid advectedSurface = new Grid();

        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
                // --- Semi-Lagrangian advection on Q ---
                Cell averageUCurCell = averageBulk.getCell(x, y);

                // Trace back to starting position
                // float xPosQ = (float) Math.clamp(x + 0.5 - (averageUCurCell.ux * Config.TIME_STEP), 0.5, WIDTH + 0.5);
                // float yPosQ = (float) Math.clamp(y + 0.5 - (averageUCurCell.uy * Config.TIME_STEP), 0.5, HEIGHT + 0.5);
                float xPosQ = (float) wallClamp(
                        new Vector2D(x + 0.5, y),
                        new Vector2D(-averageUCurCell.ux * Config.TIME_STEP, 0),
                        wallGrid
                ).getX();
                float yPosQ = (float) wallClamp(
                        new Vector2D(x, y + 0.5),
                        new Vector2D(0, -averageUCurCell.uy * Config.TIME_STEP),
                        wallGrid
                ).getY();

                int leftPos = (int) Math.round(Math.floor(xPosQ - 0.5));
                if (wallGrid.getCell(leftPos, y).h > 0)
                    leftPos++;

                int rightPos = (int) Math.round(Math.ceil(xPosQ - 0.5));

                int upPos = (int) Math.round(Math.floor(yPosQ - 0.5));
                if (wallGrid.getCell(x, upPos).h > 0)
                    upPos++;

                int downPos = (int) Math.round(Math.ceil(yPosQ - 0.5));

                // int leftPos = (int) Math.max(, 1);
                // int rightPos = (int) Math.min(, WIDTH);
                // int upPos = (int) Math.max(, 1);
                // int downPos = (int) Math.min(, HEIGHT);

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
                Cell newUCurCell = newBulk.getCell(x, y);
                Cell newULeftCell = newBulk.getCell(x - 1, y);
                Cell newUTopCell = newBulk.getCell(x, y - 1);

                float horizontalU = (newUCurCell.ux + newULeftCell.ux) / 2;
                float verticalU = (newUCurCell.uy + newUTopCell.uy) / 2;

                // Trace back to starting position
                // Transport forward in time only half a time step
                // float xPosH = (float) Math.clamp(x - (horizontalU * Config.TIME_STEP / 2), 0.5, WIDTH + 0.5);
                // float yPosH = (float) Math.clamp(y - (verticalU * Config.TIME_STEP / 2), 0.5, HEIGHT + 0.5);

                Vector2D posH = wallClamp(
                        new Vector2D(x, y),
                        new Vector2D(horizontalU * Config.TIME_STEP / 2, verticalU * Config.TIME_STEP / 2).scalarMultiply(-1),
                        wallGrid
                );

                // int xMin = (int) Math.max(, 1);
                // int xMax = (int) Math.min(, WIDTH);
                // int yMin = (int) Math.max(, 1);
                // int yMax = (int) Math.min(, HEIGHT);

                int xMin = (int) Math.round(Math.floor(posH.getX()));
                if (wallGrid.getCell(xMin, y).h > 0)
                    xMin++;

                int xMax = (int) Math.round(Math.ceil(posH.getX()));
                if (wallGrid.getCell(xMax, y).h > 0)
                    xMax--;

                int yMin = (int) Math.round(Math.floor(posH.getY()));
                if (wallGrid.getCell(x, yMin).h > 0)
                    yMin++;

                int yMax = (int) Math.round(Math.ceil(posH.getY()));
                if (wallGrid.getCell(x, yMax).h > 0)
                    yMax--;

                float topLeftVal = dampedSurface.getCell(xMin, yMin).h;
                float topRightVal = dampedSurface.getCell(xMax, yMin).h;
                float bottomLeftVal = dampedSurface.getCell(xMin, yMax).h;
                float bottomRightVal = dampedSurface.getCell(xMax, yMax).h;

                float lerpYMin = lerp(topLeftVal, topRightVal, (float) posH.getX() - xMin);
                float lerpYMax = lerp(bottomLeftVal, bottomRightVal, (float) posH.getX() - xMin);

                advectedCell.h = lerp(lerpYMin, lerpYMax, (float) posH.getY() - yMin);
            }
        }

        return advectedSurface;
    }

    private static Vector2D wallClamp(Vector2D startPos, Vector2D diff, WallGrid walls) {
        float numSteps = 5;

        for (int i = 1; i <= numSteps; i++) {
            Vector2D newPos = startPos.add(i / numSteps, diff);

            int x = Math.clamp(Math.round(newPos.getX()), 0, WIDTH + 1);
            int y = Math.clamp(Math.round(newPos.getY()), 0, HEIGHT + 1);
            if (walls.getCell(x, y).h > 0) {
                return startPos.add((i - 1) / numSteps, diff);
            }
        }
        return startPos.add(diff);
    }
}
