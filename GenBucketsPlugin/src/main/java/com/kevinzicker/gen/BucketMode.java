package com.kevinzicker.gen;

public enum BucketMode {
    VERTICAL_UP,
    VERTICAL_DOWN,
    HORIZONTAL_16,
    HORIZONTAL;

    public boolean isHorizontal() {
        return this == HORIZONTAL || this == HORIZONTAL_16;
    }
}
