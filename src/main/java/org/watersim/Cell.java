package org.watersim;

public class Cell {

    public float h;
    public float qx, qy;
    public float ux, uy;
    public float divQ;
    public float divU;

    public Cell copy() {
        var copy = new Cell();

        copy.h = h;
        copy.qx = qx;
        copy.qy = qy;
        copy.ux = ux;
        copy.uy = uy;
        copy.divQ = divQ;
        copy.divU = divU;

        return copy;
    }
}
