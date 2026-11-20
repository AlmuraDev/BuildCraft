/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics.zone;

/** A mutable pair of ints (1.12.2 used javax.vecmath's Point2i, which 26.3 doesn't ship). */
public final class Point2i {
    public int x, y;

    public Point2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void add(Point2i other) {
        x += other.x;
        y += other.y;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Point2i && ((Point2i) o).x == x && ((Point2i) o).y == y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
