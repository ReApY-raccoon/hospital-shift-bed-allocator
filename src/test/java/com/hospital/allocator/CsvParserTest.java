package com.hospital.allocator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hospital.allocator.model.Models.*;
import com.hospital.allocator.util.CsvParser;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvParserTest {

    @Test
    void parsesValidNurses() {
        List<Nurse> n = CsvParser.parseNurses("id,skill,maxShifts,availableShifts\nN1,ICU,2,S1;S2\n");
        assertEquals(1, n.size());
        assertEquals(List.of("S1", "S2"), n.get(0).availableShifts());
    }

    @Test
    void parsesValidShiftsWithWindowsLineEndings() {
        List<Shift> s = CsvParser.parseShifts("id,requiredSkill,headcount\r\nS1,ANY,2\r\nS2,ICU,1\r\n");
        assertEquals(2, s.size());
        assertEquals(2, s.get(0).headcount());
    }

    @Test
    void emptyFileIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> CsvParser.parseNurses(""));
    }

    @Test
    void headerOnlyFileIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> CsvParser.parseShifts("id,requiredSkill,headcount\n"));
    }

    @Test
    void wrongHeaderIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> CsvParser.parseShifts("a,b,c\nS1,ANY,1\n"));
    }

    @Test
    void badNumberReportsTheLine() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> CsvParser.parseShifts("id,requiredSkill,headcount\nS1,ANY,1\nS2,ANY,two\n"));
        assertTrue(e.getMessage().contains("line 3"));
    }
}
