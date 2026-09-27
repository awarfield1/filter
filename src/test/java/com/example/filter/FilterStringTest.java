package com.example.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static com.example.filter.Filters.and;
import static com.example.filter.Filters.equalTo;
import static com.example.filter.Filters.greaterThan;
import static com.example.filter.Filters.matchesRegex;
import static com.example.filter.Filters.not;
import static com.example.filter.Filters.or;
import static com.example.filter.Filters.present;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterStringTest {

    @Test
    void rendersTheCanonicalForm() {
        Filter filter = and(equalTo("role", "administrator"), greaterThan("age", "30"));

        assertEquals("(&(role=administrator)(age>30))", filter.toFilterString());
    }

    @Test
    void rendersEveryFilterType() {
        assertEquals("(true)", Filters.alwaysTrue().toFilterString());
        assertEquals("(false)", Filters.alwaysFalse().toFilterString());
        assertEquals("(email=*)", present("email").toFilterString());
        assertEquals("(role=admin)", equalTo("role", "admin").toFilterString());
        assertEquals("(role!=admin)", Filters.notEqualTo("role", "admin").toFilterString());
        assertEquals("(age<30)", Filters.lessThan("age", "30").toFilterString());
        assertEquals("(age<=30)", Filters.lessThanOrEqualTo("age", "30").toFilterString());
        assertEquals("(age>30)", greaterThan("age", "30").toFilterString());
        assertEquals("(age>=30)", Filters.greaterThanOrEqualTo("age", "30").toFilterString());
        assertEquals("(surname~=^bl)", matchesRegex("surname", "^bl").toFilterString());
        assertEquals("(!(age>30))", not(greaterThan("age", "30")).toFilterString());
        assertEquals("(|(a=1)(b=2))", or(equalTo("a", "1"), equalTo("b", "2")).toFilterString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "(true)",
            "(false)",
            "(email=*)",
            "(role=administrator)",
            "(role!=guest)",
            "(age<30)",
            "(age<=30)",
            "(age>30)",
            "(age>=30)",
            "(surname~=^bl.*gs$)",
            "(!(role=administrator))",
            "(&(role=administrator)(age>30))",
            "(|(role=administrator)(role=auditor))",
            "(&(|(role=administrator)(role=auditor))(!(status=disabled))(email=*))"
    })
    @DisplayName("parse(toFilterString(f)) == f for every supported construct")
    void roundTrips(String filterString) {
        Filter parsed = Filter.parse(filterString);

        assertEquals(filterString, parsed.toFilterString());
        assertEquals(parsed, Filter.parse(parsed.toFilterString()));
    }

    @Test
    void parsedFiltersBehaveIdenticallyToProgrammaticallyBuiltOnes() {
        Filter built = and(equalTo("role", "administrator"), greaterThan("age", "30"));
        Filter parsed = Filter.parse("(&(role=administrator)(age>30))");

        assertEquals(built, parsed);

        Map<String, String> user = Map.of("role", "administrator", "age", "35");
        assertEquals(built.matches(user), parsed.matches(user));
        assertTrue(parsed.matches(user));
    }

    @Test
    void escapesAndUnescapesSyntaxCharactersInValues() {
        Filter filter = equalTo("note", "a (b) c\\d");

        assertEquals("(note=a \\(b\\) c\\\\d)", filter.toFilterString());
        assertEquals(filter, Filter.parse(filter.toFilterString()));
    }

    @Test
    void escapesAStarOnlyWhenItWouldReadAsThePresenceForm() {
        assertEquals("(note=\\*)", equalTo("note", "*").toFilterString());
        assertEquals("(note=*joe)", equalTo("note", "*joe").toFilterString());

        assertEquals(equalTo("note", "*"), Filter.parse(equalTo("note", "*").toFilterString()));
        assertEquals(equalTo("note", "*joe"), Filter.parse(equalTo("note", "*joe").toFilterString()));
    }

    @Test
    void regexValuesContainingSyntaxCharactersRoundTrip() {
        Filter filter = matchesRegex("surname", "^(bl|sm)\\w*s$");

        assertEquals(filter, Filter.parse(filter.toFilterString()));
        assertTrue(filter.matches(Map.of("surname", "Bloggs")));
        assertTrue(Filter.parse(filter.toFilterString()).matches(Map.of("surname", "Bloggs")));
    }

    @Test
    void aStarIsOnlyPresenceWhenItIsTheWholeValue() {
        assertEquals(present("email"), Filter.parse("(email=*)"));
        assertEquals(equalTo("email", "*joe"), Filter.parse("(email=\\*joe)"));
    }

    @Test
    void whitespaceBetweenElementsIsIgnored() {
        assertEquals(
                and(equalTo("a", "1"), equalTo("b", "2")),
                Filter.parse("( &  (a=1)  (b=2) )"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "role=administrator",
            "(role=administrator",
            "(role)",
            "(&)",
            "(&(a=1)",
            "(role=admin))",
            "(role~admin)",
            "(role@admin)",
            "(=admin)",
            "(surname~=[unclosed)"
    })
    void rejectsMalformedInput(String filterString) {
        assertThrows(FilterParseException.class, () -> Filter.parse(filterString));
    }

    @Test
    void parseErrorsReportThePosition() {
        FilterParseException failure =
                assertThrows(FilterParseException.class, () -> Filter.parse("(&(a=1)(b)"));

        assertEquals("(&(a=1)(b)", failure.getInput());
        assertTrue(failure.getPosition() > 0);
        assertTrue(failure.getMessage().contains("at index"));
    }
}
