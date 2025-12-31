package ru.intelinfo.inczilla.mvc.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArchiveDatasetDateExtractor {
    private static final Pattern DATE_PATTERN = Pattern.compile("data-(\\d{8})-");
    private static final DateTimeFormatter BASIC = DateTimeFormatter.BASIC_ISO_DATE;

    public static LocalDate extractFromArchiveUrl(String archiveUrl) {
        if (archiveUrl == null) return null;

        Matcher m = DATE_PATTERN.matcher(archiveUrl);
        if (!m.find()) return null;

        String yyyymmdd = m.group(1);
        return LocalDate.parse(yyyymmdd, BASIC);
    }
}