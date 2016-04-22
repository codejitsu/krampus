CREATE KEYSPACE IF NOT EXISTS wiki
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE wiki;

CREATE TABLE edits (
  year              int,
  month             int,
  day               int,
  hour              int,
  minute            int,
  weekday           int,
  time              int,
  ts                timestamp,
  uid               uuid,
  channel           text,
  flags             list<text>,
  diff_url          text,
  added             int,
  deleted           int,
  comment           text,
  is_unpatrolled    boolean,
  is_new            boolean,
  is_minor          boolean,
  delta             int,
  namespace         text,
  pid               uuid,
  PRIMARY KEY ((year, month), day, time, uid)
) WITH CLUSTERING ORDER BY (day ASC, time ASC, uid ASC);

CREATE TABLE users (
  uid   uuid,
  name  text,
  robot boolean,
  PRIMARY KEY (uid)
);

CREATE TABLE pages (
  pid   uuid,
  name  text,
  url   text,
  PRIMARY KEY (pid)
);
