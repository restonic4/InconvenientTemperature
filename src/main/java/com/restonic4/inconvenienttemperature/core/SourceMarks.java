package com.restonic4.inconvenienttemperature.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SourceMarks {
    private SourceMarks() {}

    /** Every room mark is kept; only the radiant marks that reach the player most are, and a block in both lists becomes one mark. */
    public static List<SourceMark> merge(List<SourceMark> room, List<SourceMark> radiant, int radiantLimit) {
        List<SourceMark> strongest = new ArrayList<>(radiant);
        strongest.sort(Comparator.comparingDouble((SourceMark mark) -> Math.abs(mark.delivered())).reversed());

        Map<Long, SourceMark> byBlock = new LinkedHashMap<>();
        for (SourceMark mark : room) {
            byBlock.merge(RoomAnalyzer.pack(mark.x(), mark.y(), mark.z()), mark, SourceMark::plus);
        }

        for (SourceMark mark : strongest.subList(0, Math.min(radiantLimit, strongest.size()))) {
            byBlock.merge(RoomAnalyzer.pack(mark.x(), mark.y(), mark.z()), mark, SourceMark::plus);
        }

        return List.copyOf(byBlock.values());
    }
}
