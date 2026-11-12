package com.apparel.commerce.common;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtil {
    private SlugUtil() {}

    public static String slugify(String input) {
        String s = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return s.isEmpty() ? "item" : s;
    }
}
