package com.restonic4.inconvenienttemperature.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SourceMarksTest {
    private static SourceMark radiant(int x, float strength, float delivered) {
        return new SourceMark(x, 0, 0, strength, delivered, 0f);
    }

    private static SourceMark room(int x, float power) {
        return new SourceMark(x, 0, 0, 0f, 0f, power);
    }

    @Test
    void aFurnaceOutsideAnyRoomStillGetsAMarkWithBothNumbers() {
        List<SourceMark> marks = SourceMarks.merge(List.of(), List.of(radiant(3, 14f, 6.2f)), 24);

        assertEquals(1, marks.size());
        assertEquals(14f, marks.get(0).radiant(), 0f);
        assertEquals(6.2f, marks.get(0).delivered(), 0f);
    }

    @Test
    void aBlockThatIsBothRadiantAndInTheRoomBecomesOneMark() {
        List<SourceMark> marks = SourceMarks.merge(List.of(room(3, 20f)), List.of(radiant(3, 14f, 6.2f)), 24);

        assertEquals(1, marks.size());
        assertTrue(marks.get(0).hasRadiant() && marks.get(0).hasRoom());
        assertEquals(20f, marks.get(0).room(), 0f);
        assertEquals(14f, marks.get(0).radiant(), 0f);
    }

    @Test
    void onlyTheRadiantSourcesThatReachYouMostSurviveTheLimit() {
        List<SourceMark> radiant = List.of(radiant(1, 14f, 0.5f), radiant(2, 14f, 9f), radiant(3, -1.5f, -4f));
        List<SourceMark> marks = SourceMarks.merge(List.of(), radiant, 2);

        assertEquals(List.of(2, 3), marks.stream().map(SourceMark::x).toList());
    }

    @Test
    void theLimitNeverDropsRoomMarks() {
        List<SourceMark> rooms = List.of(room(1, 20f), room(2, -5f), room(3, 8f));

        assertEquals(3, SourceMarks.merge(rooms, List.of(), 0).size());
    }

    @Test
    void heatingFollowsTheRadiantSignThenTheRoomSign() {
        assertTrue(radiant(0, 14f, 1f).heating());
        assertFalse(radiant(0, -1.5f, -1f).heating());
        assertTrue(room(0, 20f).heating());
        assertFalse(room(0, -5f).heating());
    }
}
