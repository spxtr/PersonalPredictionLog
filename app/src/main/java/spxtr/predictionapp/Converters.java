package spxtr.predictionapp;

import androidx.room.TypeConverter;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class Converters {
    private static DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @TypeConverter
    public static String fromOffsetDateTime(OffsetDateTime dt) {
        if (dt == null) {
            return "";
        }
        return dt.format(formatter);
    }

    @TypeConverter
    public static OffsetDateTime toOffsetDateTime(String s) {
        if (s.length() == 0) {
            return null;
        }
        return OffsetDateTime.from(formatter.parse(s));
    }
}
