package org.watersim;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class Decomposer {

    public static Pair<Grid, Grid> decomposeBulkOnly(Grid grid) {
        return new ImmutablePair<>(grid.copy(), new Grid(grid.WIDTH, grid.HEIGHT));
    }

    public static Pair<Grid, Grid> decomposeSurfaceOnly(Grid grid) {
        return new ImmutablePair<>(new Grid(grid.WIDTH, grid.HEIGHT), grid.copy());
    }
}
