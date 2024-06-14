package org.fenixedu.treasury.dates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(FenixFrameworkRunner.class)
public class DateTimesAndTimeZonesTest {

    @BeforeClass
    public static void beforeClass() {
    }

    @Test
    public void compareDateTimeFieldsRemovingMilliseconds() {
        DateTime dateTimeWithMillis = DateTime.now();
        DateTime dateTimeWithoutMillis = dateTimeWithMillis.withMillisOfSecond(0);

        assertEquals(dateTimeWithMillis.getYear(), dateTimeWithoutMillis.getYear());
        assertEquals(dateTimeWithMillis.getMonthOfYear(), dateTimeWithoutMillis.getMonthOfYear());
        assertEquals(dateTimeWithMillis.getDayOfMonth(), dateTimeWithoutMillis.getDayOfMonth());
        assertEquals(dateTimeWithMillis.getHourOfDay(), dateTimeWithoutMillis.getHourOfDay());
        assertEquals(dateTimeWithMillis.getMinuteOfHour(), dateTimeWithoutMillis.getMinuteOfHour());
        assertEquals(dateTimeWithMillis.getSecondOfMinute(), dateTimeWithoutMillis.getSecondOfMinute());
        assertEquals(dateTimeWithMillis.getZone(), dateTimeWithoutMillis.getZone());
    }

    @Test
    public void compareDateTimesWithSameHourButDifferentTimeZonesInWinter() {
        DateTime dateTimeInUTC = new DateTime(2022, 1, 1, 1, 10, 10, DateTimeZone.UTC);
        DateTime dateTimeInUTCPlusOne = new DateTime(2022, 1, 1, 1, 10, 10, DateTimeZone.forID("Europe/Lisbon"));

        assertTrue(dateTimeInUTC.compareTo(dateTimeInUTCPlusOne) == 0);

        DateTimeZone serverTimezone = DateTimeZone.forID("Europe/Lisbon");

        int serverOffsetMillis = serverTimezone.getOffset(dateTimeInUTC);

        // Convert the DateTime to the server's timezone dynamically
        DateTime dateTimeServerTimezone = dateTimeInUTC.withZone(DateTimeZone.forOffsetMillis(serverOffsetMillis));

        assertTrue(dateTimeInUTC.compareTo(dateTimeServerTimezone) == 0);
    }

    @Test
    public void compareDateTimesWithSameHourButDifferentTimeZonesInSummer() {
        DateTime dateTimeInUTC = new DateTime(2022, 7, 1, 1, 10, 10, DateTimeZone.UTC);
        DateTime dateTimeInUTCPlusOne = new DateTime(2022, 7, 1, 2, 10, 10, DateTimeZone.forID("Europe/Lisbon"));

        assertTrue(dateTimeInUTC.compareTo(dateTimeInUTCPlusOne) == 0);

        DateTimeZone serverTimezone = DateTimeZone.forID("Europe/Lisbon");

        int serverOffsetMillis = serverTimezone.getOffset(dateTimeInUTC);

        // Convert the DateTime to the server's timezone dynamically
        DateTime dateTimeServerTimezone = dateTimeInUTC.withZone(DateTimeZone.forOffsetMillis(serverOffsetMillis));

        assertTrue(dateTimeInUTC.compareTo(dateTimeServerTimezone) == 0);
    }

}
