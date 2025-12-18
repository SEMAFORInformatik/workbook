package ch.semafor.intens.ws.utils;

import java.util.Date;

import org.joda.time.format.ISODateTimeFormat;

public class DateTimeFormatter {
  // convert a date or date/time string to a localized Date
  // only the year is mandatory
  // examples (with local timezone CET / CEST)

  // including timezone (requires everything up to minutes)
  // 2018-08-09T12:53:57.469+0100 > 2018-08-09 13:53:57.469 CEST
  // 2018-08-09T12:53:57+0100     > 2018-08-09 13:53:57.000 CEST  milliseconds default to 000
  // 2018-08-09T12:53+0100        > 2018-08-09 13:53:00.000 CEST  seconds default to 00

  // without timezone
  // 2018-08-09T12:53:57.469      > 2018-08-09 12:53:57.469 CEST  timezone defaults to local timezone
  // 2018-08-09T12:53:57          > 2018-08-09 12:53:57.000 CEST  milliseconds default to 000
  // 2018-08-09T12:53             > 2018-08-09 12:53:00.000 CEST  seconds default to 00
  // 2018-08-09T12                > 2018-08-09 12:00:00.000 CEST  minutes default to 00
  // 2018-08-09                   > 2018-08-09 00:00:00.000 CEST  hour defaults to 00
  // 2018-08                      > 2018-08-01 00:00:00.000 CEST  day defaults to 01
  // 2018                         > 2018-01-01 00:00:00.000 CEST  month defaults to 01
  static org.joda.time.format.DateTimeFormatter fmt = ISODateTimeFormat.dateOptionalTimeParser();

  public static Date convert( String dstr ){
    if (dstr.length() == 0) return null;
    return fmt.parseDateTime(dstr).toDate();
  }
}
