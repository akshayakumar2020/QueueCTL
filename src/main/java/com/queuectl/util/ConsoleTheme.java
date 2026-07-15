package com.queuectl.util;

import picocli.CommandLine.Help.Ansi;

import java.util.ArrayList;
import java.util.List;

public final class ConsoleTheme {
    private ConsoleTheme() {
    }

    public static String accent(String text) {
        return Ansi.AUTO.string("@|fg(cyan),bold " + escape(text) + "|@");
    }

    public static String ok(String text) {
        return Ansi.AUTO.string("@|fg(cyan) " + escape(text) + "|@");
    }

    public static String error(String text) {
        return Ansi.AUTO.string("@|fg(red),bold " + escape(text) + "|@");
    }

    public static String muted(String text) {
        return Ansi.AUTO.string("@|fg(black) " + escape(text) + "|@");
    }

    public static String table(List<String> headers, List<List<String>> rows) {
        List<Integer> widths = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            int width = headers.get(i).length();
            for (List<String> row : rows) {
                width = Math.max(width, row.get(i) == null ? 0 : row.get(i).length());
            }
            widths.add(Math.min(Math.max(width, 4), 72));
        }

        StringBuilder out = new StringBuilder();
        appendRow(out, headers, widths);
        List<String> separators = widths.stream().map("-"::repeat).toList();
        appendRow(out, separators, widths);
        for (List<String> row : rows) {
            appendRow(out, row, widths);
        }
        return out.toString();
    }

    private static void appendRow(StringBuilder out, List<String> cells, List<Integer> widths) {
        for (int i = 0; i < cells.size(); i++) {
            String value = cells.get(i) == null ? "" : cells.get(i);
            if (value.length() > widths.get(i)) {
                value = value.substring(0, Math.max(0, widths.get(i) - 1)) + ".";
            }
            out.append(String.format("%-" + widths.get(i) + "s", value));
            if (i < cells.size() - 1) {
                out.append("  ");
            }
        }
        out.append(System.lineSeparator());
    }

    private static String escape(String text) {
        return text.replace("|", "\\|");
    }
}
