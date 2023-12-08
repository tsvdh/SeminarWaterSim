package org.watersim.algorithm;

import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.watersim.grid.Cell;
import org.watersim.util.Config;
import org.watersim.grid.Grid;
import org.watersim.grid.WallGrid;

import java.nio.file.Path;

import static org.watersim.util.Config.Airy;
import static org.watersim.util.Config.HEIGHT;
import static org.watersim.util.Config.SWE;
import static org.watersim.util.Config.WIDTH;

public class Simulator {

    private Grid prevBulk;
    private Grid prevSurface;

    @Getter
    private Grid grid;

    private final WallGrid wallGrid;

    public Simulator(Path path) {
        Pair<Grid, WallGrid> input = Grid.parseInput(path);

        grid = input.getLeft();
        wallGrid = input.getRight();

        Pair<Grid, Grid> prevGrids = Decomposer.decompose(grid.copy(), wallGrid);
        prevBulk = prevGrids.getLeft();
        prevSurface = prevGrids.getRight();

        if (Config.CELL_SIZE != 1)
            throw new RuntimeException();
    }

    public Grid makeNewGrid() {
        Pair<Grid, Grid> grids;

        if (SWE && Airy)
            grids = Decomposer.decompose(grid, wallGrid);
        else if (SWE)
            grids = Decomposer.decomposeBulkOnly(grid);
        else if (Airy)
            grids = Decomposer.decomposeSurfaceOnly(grid);
        else
            throw new RuntimeException();

        Grid bulk = grids.getLeft();
        Grid surface = grids.getRight();

        // compute u velocities
        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
                Cell bulkCell = bulk.getCell(x, y);
                Pair<Float, Float> upwindH = prevBulk.getUpwindH(x, y);

                // do not compute for walls
                if (wallGrid.canFlowRight(x, y))
                    bulkCell.ux = upwindH.getLeft() == 0 ? 0 : bulkCell.qx / upwindH.getLeft();
                if (wallGrid.canFlowDown(x, y))
                    bulkCell.uy = upwindH.getRight() == 0 ? 0 : bulkCell.qy / upwindH.getRight();
            }
        }

        // Compute surface and bulk components
        Grid newBulk = BulkFlowComputer.computeNewBulkUAndQ(grid, bulk, wallGrid);
        Grid newSurface = AiryWaveComputer.computeSurfaceQ(grid, surface, prevSurface);

        // Transport surface through bulk flow
        Grid transportedSurface = SurfaceTransporter.transportSurface(surface, newSurface, bulk, newBulk);

        Grid newGrid = new Grid();
        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
                Cell newCell = newGrid.getCell(x, y);
                Cell newBulkCell = newBulk.getCell(x, y);
                Cell transportedCell = transportedSurface.getCell(x, y);
                newCell.qx = newBulkCell.qx + transportedCell.qx;
                newCell.qy = newBulkCell.qy + transportedCell.qy;
            }
        }
        newGrid.clampQ(grid.computeUpwindH());

        // Compute new divergence for height update
        Grid tempGrid = new Grid();
        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
                Cell tempCell = tempGrid.getCell(x, y);
                Cell newCell = newGrid.getCell(x, y);
                Cell transportedCell = transportedSurface.getCell(x, y);
                Cell newBulkCell = newBulk.getCell(x, y);

                tempCell.qx = newCell.qx + transportedCell.h * newBulkCell.ux;
                tempCell.qy = newCell.qy + transportedCell.h * newBulkCell.uy;
            }
        }
        tempGrid.computeDivergence();

        // update heights with flow divergence
        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
                // TODO: perhaps only for non walls
                Cell curCell = grid.getCell(x, y);
                Cell newCell = newGrid.getCell(x, y);
                Cell tempCell = tempGrid.getCell(x, y);

                newCell.h = curCell.h - Config.TIME_STEP * tempCell.divQ;
            }
        }

        prevBulk = bulk;
        prevSurface = surface;
        grid = newGrid;

        return grid;
    }
}
