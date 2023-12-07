package org.watersim.grid;

import java.nio.file.Path;

public class WallGrid extends Grid {

    public WallGrid(int width, int height) {
        super(width, height);
        createBoundaryWalls();
    }

    public WallGrid(String input) {
        super(input);
        createBoundaryWalls();
    }

    public WallGrid(Path path) {
        super(path);
        createBoundaryWalls();
    }

    public void createBoundaryWalls() {
        for (int y = 0; y <= HEIGHT + 1; y++) {
            for (int x = 0; x <= WIDTH + 1; x++) {
                if (x == 0 || y == 0 || x == HEIGHT + 1 || y == WIDTH + 1) {
                    getCell(x, y).h = 1f;
                }
            }
        }
    }

    public boolean canFlowRight(int x, int y) {
        return getCell(x, y).h == 0 && getCell(x + 1, y).h == 0;
    }

    public boolean canFlowDown(int x, int y) {
        return getCell(x, y).h == 0 && getCell(x, y + 1).h == 0;
    }

    public boolean canFlowLeft(int x, int y) {
        return getCell(x, y).h == 0 && getCell(x - 1, y).h == 0;
    }

    public boolean canFlowUp(int x, int y) {
        return getCell(x, y).h == 0 && getCell(x, y - 1).h == 0;
    }
}
