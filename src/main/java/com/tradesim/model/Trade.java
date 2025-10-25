package com.tradesim.model;

public record Trade(double rrr, boolean win) implements Comparable<Trade> {
    public static Trade from(double rrr, boolean win) {
        return new Trade(rrr, win);
    }

    @Override
    public int compareTo(Trade other) {
        // Wins come before losses
        if (this.win && !other.win) return -1;
        if (!this.win && other.win) return 1;

        // If both are wins or both are losses, compare by rrr (descending)
        return Double.compare(other.rrr, this.rrr);
    }
}
