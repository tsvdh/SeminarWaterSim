package org.watersim;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.watersim.Config.HEIGHT;
import static org.watersim.Config.WIDTH;

public class Grid {

    private Cell[][] cells;

    public int WIDTH, HEIGHT;

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

    public Grid(String input) {
        init(input.lines());
    }

    public Grid(Path path) {
        try (var reader = Files.newBufferedReader(path)) {
            init(reader.lines());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Cell getCell(int x, int y) {
        return cells[y][x];
    }

    public void setCell(int x, int y, Cell newCell) {
        cells[y][x] = newCell;
    }

    public void init(Stream<String> input) {
        List<String[]> charsList = input
                .map(x -> x.split(" "))
                .toList();

        List<String[]> heightsList = new ArrayList<>();
        List<String[]> velocitiesXList = new ArrayList<>();
        List<String[]> velocitiesYList = new ArrayList<>();

        int listIndex = 0;
        for (String[] line : charsList) {
            if (line[0].equals("-")) {
                listIndex++;
                continue;
            }

            switch (listIndex) {
                case 0 -> heightsList.add(line);
                case 1 -> velocitiesXList.add(line);
                case 2 -> velocitiesYList.add(line);
            }
        }

        this.WIDTH = heightsList.get(0).length;
        this.HEIGHT = heightsList.size();

        cells = new Cell[HEIGHT + 2][WIDTH + 2];

        for (int y = 0; y <= HEIGHT + 1; y++) {
            for (int x = 0; x <= WIDTH + 1; x++) {
                cells[y][x] = new Cell();

                if (y > 0 && x > 0 && y <= HEIGHT && x <= WIDTH) {
                    cells[y][x].h = Float.parseFloat(heightsList.get(y - 1)[x - 1]);

                    if (!velocitiesXList.isEmpty()) {
                        cells[y][x].qx = Float.parseFloat(velocitiesXList.get(y - 1)[x - 1]);
                        cells[y][x].qy = Float.parseFloat(velocitiesYList.get(y - 1)[x - 1]);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();

        for (int i = 0; i < 3; i++) {
            for (int y = 1; y <= HEIGHT; y++) {
                for (int x = 1; x <= WIDTH; x++) {
                    Cell cell = getCell(x, y);
                    builder.append(switch (i) {
                        case 0 -> cell.h;
                        case 1 -> cell.qx;
                        case 2 -> cell.qy;
                        default -> throw new RuntimeException();
                    });
                    builder.append(" ");
                }
                builder.append("\n");
            }
            builder.append("-\n");
        }

        return builder.toString();
    }

    public void dump(String path) {
        Path filePath = Paths.get(path);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            writer.write(toString());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public Pair<Float, Float> getUpwindH(int x, int y) {
        Cell curCell = getCell(x, y);

        float thisH = curCell.h;
        float xH = getCell(x + 1, y).h;
        float yH = getCell(x, y + 1).h;

        float upwindXH, upwindYH;

        float signX = curCell.ux != 0 ? curCell.ux: curCell.qx;
        if (signX > 0) {
            upwindXH = thisH;
        } else if (signX < 0) {
            upwindXH = xH;
        } else {
            upwindXH = Math.max(thisH, xH);
        }

        float signY = curCell.uy != 0 ? curCell.uy : curCell.qy;
        if (signY > 0) {
            upwindYH = thisH;
        } else if (signY < 0) {
            upwindYH = yH;
        } else {
            upwindYH = Math.max(thisH, yH);
        }

        return new ImmutablePair<>(upwindXH, upwindYH);
    }

    public Pair<Float, Float> getAverageQ(int x, int y) {
        Cell cell = getCell(x, y);
        Cell topCell = getCell(x, y - 1);
        Cell leftCell = getCell(x - 1, y);

        float averageXQ = (leftCell.qx + cell.qx) / 2;
        float averageYQ = (topCell.qy + cell.qy) / 2;

        return new ImmutablePair<>(averageXQ, averageYQ);
    }

    public float totalVolume() {
        float total = 0;

        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
                total += getCell(x, y).h;
            }
        }

        return total;
    }

    public static Pair<Grid, WallGrid> parseInput(Path path) {
        StringBuilder water = new StringBuilder();
        StringBuilder walls = new StringBuilder();

        try (var reader = Files.newBufferedReader(path)) {
            boolean readingWater = true;

            for (String line : reader.lines().toList()) {
                if (readingWater) {
                    if (line.equals("-"))
                        readingWater = false;
                    else {
                        water.append(line);
                        water.append('\n');
                    }
                } else {
                    walls.append(line);
                    walls.append('\n');
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new ImmutablePair<>(new Grid(water.toString()), new WallGrid(walls.toString()));
    }

    public void computeDivergence() {
        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
                Cell cell = getCell(x, y);
                Cell leftCell = getCell(x - 1, y);
                Cell upCell = getCell(x, y - 1);

                cell.divQ = (cell.qx - leftCell.qx) / Config.CELL_SIZE + (cell.qy - upCell.qy) / Config.CELL_SIZE;
                cell.divU = (cell.ux - leftCell.ux) / Config.CELL_SIZE + (cell.uy - upCell.uy) / Config.CELL_SIZE;
            }
        }
    }

    public Pair<Grid, Grid> computeUpwindH() {
        Grid heightsX = new Grid(WIDTH, HEIGHT);
        Grid heightsY = new Grid(WIDTH, HEIGHT);

        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
                Cell cellX = heightsX.getCell(x, y);
                Cell cellY = heightsY.getCell(x, y);

                Pair<Float, Float> heights = getUpwindH(x, y);
                cellX.h = heights.getLeft();
                cellY.h = heights.getRight();
            }
        }

        //noinspection SuspiciousNameCombination
        return new ImmutablePair<>(heightsX, heightsY);
    }

    public void clampU() {
        for (int y = 1; y < HEIGHT; y++) {
            for (int x = 1; x < WIDTH; x++) {
                Cell cell = getCell(x, y);

                cell.ux = clampU(cell.ux);
                cell.uy = clampU(cell.uy);
            }
        }
    }

    public void clampQ(Pair<Grid, Grid> upwindHeights) {
        Grid heightsX = upwindHeights.getLeft();
        Grid heightsY = upwindHeights.getRight();

        for (int y = 1; y < HEIGHT; y++) {
            for (int x = 1; x < WIDTH; x++) {
                Cell cell = getCell(x, y);
                cell.qx = clampQ(cell.qx, heightsX.getCell(x, y).h);
                cell.qy = clampQ(cell.qy, heightsY.getCell(x, y).h);
            }
        }
    }

    private static float clampU(float u) {
        float uMax = Config.CELL_SIZE / (4 * Config.TIME_STEP);

        return Math.clamp(u, -uMax, uMax);
    }

    private static float clampQ(float q, float h) {
        float qMax = Math.abs(h) * Config.CELL_SIZE / (4 * Config.TIME_STEP);

        return Math.clamp(q, -qMax, qMax);
    }
}
