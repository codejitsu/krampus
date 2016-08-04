CREATE KEYSPACE IF NOT EXISTS wiki WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 2};

USE wiki;

CREATE TABLE edits (
  id                uuid,
  year              int,
  month             int,
  day               int,
  hour              int,
  minute            int,
  weekday           int,
  time              int, // the time of the day in seconds
  day_minute        int,
  ts                timestamp,
  channel           text,
  flags             list<text>,
  diff_url          text,
  added             int,
  deleted           int,
  comment           text,
  is_unpatrolled    boolean,
  is_new            boolean,
  is_minor          boolean,
  is_robot          boolean,
  delta             int,
  namespace         text,
  user              text,
  page              text,
  PRIMARY KEY ((year, month), day, time, user, id)
) WITH CLUSTERING ORDER BY (day ASC, time ASC);