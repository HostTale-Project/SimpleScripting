package com.hosttale.simplescripting.mod.runtime.db;

import java.util.Locale;
import java.util.Set;

/**
 * Performs lightweight validation on SQL strings before handing them to SQLite.
 * The intent is to block features that could break isolation or escape to the filesystem.
 */
final class SqlValidator {

    private static final Set<String> FORBIDDEN = Set.of(
            "attach",
            "detach",
            "pragma",
            "vacuum",
            "load_extension"
    );

    private SqlValidator() {
    }

    public static void validate(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL statement is required.");
        }
        String forbidden = findForbiddenToken(sql);
        if (forbidden != null) {
            throw new IllegalArgumentException("SQL statement uses a restricted feature: " + forbidden);
        }
    }

    private static String findForbiddenToken(String sql) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        StringBuilder token = new StringBuilder();
        String lower = sql.toLowerCase(Locale.ROOT);

        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                token.append(c);
            } else {
                if (token.length() > 0) {
                    String maybe = token.toString();
                    if (FORBIDDEN.contains(maybe)) {
                        return maybe;
                    }
                    token.setLength(0);
                }
            }
        }

        if (token.length() > 0 && FORBIDDEN.contains(token.toString())) {
            return token.toString();
        }
        return null;
    }
}
