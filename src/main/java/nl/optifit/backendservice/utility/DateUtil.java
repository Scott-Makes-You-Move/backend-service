package nl.optifit.backendservice.utility;

import com.microsoft.graph.models.DateTimeTimeZone;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    public static final String DEFAULT_TIME_ZONE = "Europe/Amsterdam";

    private DateUtil() {
    }

    public static DateTimeTimeZone toGraphDateTime(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }

        String localDateTime = zonedDateTime.toLocalDateTime()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        DateTimeTimeZone dt = new DateTimeTimeZone();
        dt.setDateTime(localDateTime);
        dt.setTimeZone(DEFAULT_TIME_ZONE);

        return dt;
    }
}
