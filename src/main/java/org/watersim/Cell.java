package org.watersim;

public class Cell {

    public float h;
    public float qx, qy;
    public float ux, uy;

    public Cell() {
        h = 0f;
        qx = 0f;
        qy = 0f;
        ux = 0f;
        uy = 0f;
    }

    public Cell copy() {
        var copy = new Cell();

        copy.h = h;
        copy.qx = qx;
        copy.qy = qy;
        copy.ux = ux;
        copy.uy = uy;

        return copy;
    }
}
