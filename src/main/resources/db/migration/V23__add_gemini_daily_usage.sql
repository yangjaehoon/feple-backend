CREATE TABLE gemini_daily_usage (
    date  DATE NOT NULL,
    count INT  NOT NULL DEFAULT 0,
    PRIMARY KEY (date)
);
