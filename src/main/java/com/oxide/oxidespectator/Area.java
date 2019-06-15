package com.oxide.oxidespectator;

import org.bukkit.Location;

public class Area {
    private final double x_max;
    private final double z_max;
    private final double x_min;
    private final double z_min;

    private Area(double x, double z, double size) {
        this.x_max = x + size;
        this.z_max = z + size;
        this.x_min = x - size;
        this.z_min = z - size;
    }

    Area(Location location, double size) {
        this(location.getX(), location.getZ(), size);
    }

    double getX_max() {
        return this.x_max;
    }

    double getZ_max() {
        return this.z_max;
    }

    double getX_min() {
        return this.x_min;
    }

    double getZ_min() {
        return this.z_min;
    }

    public String toString() {
        return "max:" + this.x_max + " - min:" + this.x_min + "   |   min:" + this.z_max + " - max:" + this.z_min;
    }
}