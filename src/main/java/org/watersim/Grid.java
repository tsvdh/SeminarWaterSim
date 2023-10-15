package org.watersim;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Grid {

    private Cell[][] cells;

    public final int WIDTH, HEIGHT;

    public Grid(int width, int height) {
        this.WIDTH = width;
        this.HEIGHT = height;
        cells = new Cell[height + 2][width + 2];

        for (int x = 0; x <= WIDTH + 1; x++) {
            for (int y = 0; y <= HEIGHT + 1; y++) {
                cells[y][x] = new Cell();
            }
        }
    }

    public Grid(String path) {
        List<String[]> charsList;

        try (var reader = Files.newBufferedReader(Paths.get(path))) {
            charsList = reader.lines()
                    .map(x -> x.split(" "))
                    .collect(Collectors.toList());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.WIDTH = charsList.get(0).length;
        this.HEIGHT = charsList.size();

        cells = new Cell[HEIGHT + 2][WIDTH + 2];

        for (int y = 0; y <= HEIGHT + 1; y++) {
            for (int x = 0; x <= WIDTH + 1; x++) {
                cells[y][x] = new Cell();

                if (y > 0 && x > 0 && y <= HEIGHT && x <= WIDTH) {
                    cells[y][x].h = Float.parseFloat(charsList.get(y - 1)[x - 1]);
                }
            }
        }
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();

        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
                builder.append(getCell(x, y).h);
                builder.append(" ");
            }
            builder.append("\n");
        }

        return builder.toString();
    }

    public Grid copy() {
        Grid newGrid = new Grid(WIDTH, HEIGHT);

        for (int x = 0; x <= WIDTH + 1; x++) {
            for (int y = 0; y <= HEIGHT + 1; y++) {
                newGrid.setCell(x, y, this.getCell(x, y).copy());
            }
        }

        return newGrid;
    }

    public Cell getCell(int x, int y) {
        return cells[y][x];
    }

    public void setCell(int x, int y, Cell newCell) {
        cells[y][x] = newCell;
    }

    private boolean xInActive(int x) {
        return x > 0 && x <= WIDTH;
    }

    private boolean yInActive(int y) {
        return y > 0 && y <= HEIGHT;
    }

    private boolean xInBounds(int x) {
        return x >= 0 && x <= WIDTH + 1;
    }

    private boolean yInBounds(int y) {
        return y >= 0 && y <= HEIGHT + 1;
    }

    public Pair<Float, Float> getAverageH(int x, int y) {
        Float thisH = (xInActive(x) && yInActive(y)) ? getCell(x, y).h : null;
        Float xH = (xInActive(x + 1) && yInActive(y)) ? getCell(x + 1, y).h : null;
        Float yH = (xInActive(x) && yInActive(y + 1)) ? getCell(x, y + 1).h : null;

        Float averageXH, averageYH;

        if (thisH != null && xH != null) {
            averageXH = (thisH + xH) / 2;
        } else if (xH == null) {
            averageXH = thisH;
        } else {
            averageXH = xH;
        }
        if (thisH != null && yH != null) {
            averageYH = (thisH + yH) / 2;
        } else if (yH == null) {
            averageYH = thisH;
        } else {
            averageYH = yH;
        }

        return new ImmutablePair<>(averageXH, averageYH);
    }

    public Pair<Float, Float> getAverageQ(int x, int y) {
        Cell cell = getCell(x, y);
        Cell topCell = getCell(x, y - 1);
        Cell leftCell = getCell(x - 1, y);

        float averageXQ = (leftCell.qx + cell.qx) / 2;
        float averageYQ = (topCell.qy + cell.qy) / 2;

        return new ImmutablePair<>(averageXQ, averageYQ);
    }
}
