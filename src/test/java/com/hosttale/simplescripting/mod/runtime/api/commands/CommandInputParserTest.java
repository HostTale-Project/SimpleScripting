package com.hosttale.simplescripting.mod.runtime.api.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandInputParserTest {

    @Test
    void preservesWhitespaceInRawArguments() {
        CommandsApi.ParsedArgs parsed = CommandsApi.CommandInputParser.parse("/foo   bar   baz  ", java.util.List.of("foo"));

        assertArrayEquals(new String[]{"bar", "baz"}, parsed.args());
        assertEquals("   bar   baz  ", parsed.raw());
    }

    @Test
    void handlesTabsAndMixedSpacing() {
        CommandsApi.ParsedArgs parsed = CommandsApi.CommandInputParser.parse("/foo\tbar\t\tbaz", java.util.List.of("foo"));

        assertArrayEquals(new String[]{"bar", "baz"}, parsed.args());
        assertEquals("\tbar\t\tbaz", parsed.raw());
    }

    @Test
    void allowsAliasWithoutSlashAndLeadingWhitespace() {
        CommandsApi.ParsedArgs parsed = CommandsApi.CommandInputParser.parse("   alias  one two", java.util.List.of("foo", "alias"));

        assertArrayEquals(new String[]{"one", "two"}, parsed.args());
        assertEquals("  one two", parsed.raw());
    }

    @Test
    void returnsEmptyWhenNoArgumentsProvided() {
        CommandsApi.ParsedArgs parsed = CommandsApi.CommandInputParser.parse("/foo   ", java.util.List.of("foo"));

        assertArrayEquals(new String[0], parsed.args());
        assertEquals("", parsed.raw());
    }

    @Test
    void returnsEmptyWhenCommandDoesNotMatch() {
        CommandsApi.ParsedArgs parsed = CommandsApi.CommandInputParser.parse("/bar baz", java.util.List.of("foo"));

        assertArrayEquals(new String[0], parsed.args());
        assertEquals("", parsed.raw());
    }
}
