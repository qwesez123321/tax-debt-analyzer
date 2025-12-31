package ru.intelinfo.inczilla.mvc.io;

/**
 * Выделяет прямую ссылку на ZIP-архив из HTML страницы набора открытых данных.
 */
public class ArchiveUrlExtractor {

    private static final String ZIP_EXTENSION = ".zip";

    public String extractZipUrl(String html) {
        if (html == null || html.isBlank()) return null;

        for (String line : html.split("\n")) {
            String url = tryExtractZipUrl(line);
            if (url != null) return url;
        }
        return null;
    }

    private String tryExtractZipUrl(String line) {
        if (line == null || line.isBlank()) return null;

        int httpIndex = line.indexOf("http");
        if (httpIndex < 0) return null;

        int end = findUrlEnd(line, httpIndex);
        if (end <= httpIndex) return null;

        String candidate = line.substring(httpIndex, end);
        return hasZipExtension(candidate) ? candidate : null;
    }

    private int findUrlEnd(String line, int start) {
        int end = line.length();
        for (char c : new char[]{'"', '\'', ' '}) {
            int idx = line.indexOf(c, start);
            if (idx > 0 && idx < end) end = idx;
        }
        return end;
    }

    private boolean hasZipExtension(String url) {
        return url != null && url.toLowerCase().contains(ZIP_EXTENSION);
    }
}