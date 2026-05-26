CREATE DATABASE IF NOT EXISTS toutiao_demo
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE toutiao_demo;

CREATE TABLE IF NOT EXISTS news (
  id            VARCHAR(64)  PRIMARY KEY,
  category      VARCHAR(16)  NOT NULL,
  title         VARCHAR(512) NOT NULL,
  description   TEXT,
  source        VARCHAR(128),
  image_url     VARCHAR(1024),
  original_url  VARCHAR(1024),
  publish_time  DATETIME,
  layout_type   VARCHAR(32)  NOT NULL,
  fetched_at    DATETIME     NOT NULL,
  page          INT          NOT NULL,
  position      INT          NOT NULL,
  INDEX idx_cat_page (category, page, position),
  INDEX idx_publish (publish_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS category_cache_meta (
  category        VARCHAR(16) NOT NULL,
  page            INT         NOT NULL,
  last_fetched_at DATETIME    NOT NULL,
  has_more        BOOLEAN     NOT NULL DEFAULT TRUE,
  total_count     INT,
  PRIMARY KEY (category, page)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
