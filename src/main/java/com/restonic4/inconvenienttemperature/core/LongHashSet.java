package com.restonic4.inconvenienttemperature.core;

final class LongHashSet {
    private long[] keys;
    private boolean[] used;
    private int size;

    LongHashSet(int expected) {
        int capacity = Integer.highestOneBit(Math.max(16, expected * 2)) << 1;
        keys = new long[capacity];
        used = new boolean[capacity];
    }

    boolean add(long key) {
        if (size * 2 >= keys.length) grow();
        int mask = keys.length - 1;
        int slot = mix(key) & mask;
        while (used[slot]) {
            if (keys[slot] == key) return false;
            slot = (slot + 1) & mask;
        }
        used[slot] = true;
        keys[slot] = key;
        size++;
        return true;
    }

    boolean contains(long key) {
        int mask = keys.length - 1;
        int slot = mix(key) & mask;
        while (used[slot]) {
            if (keys[slot] == key) return true;
            slot = (slot + 1) & mask;
        }
        return false;
    }

    private void grow() {
        long[] oldKeys = keys;
        boolean[] oldUsed = used;
        keys = new long[oldKeys.length * 2];
        used = new boolean[oldKeys.length * 2];
        size = 0;
        for (int i = 0; i < oldKeys.length; i++) {
            if (oldUsed[i]) add(oldKeys[i]);
        }
    }

    private static int mix(long key) {
        long h = key * 0x9E3779B97F4A7C15L;
        return (int) (h ^ (h >>> 32));
    }
}
