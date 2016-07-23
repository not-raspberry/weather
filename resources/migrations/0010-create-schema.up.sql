CREATE TABLE conditions (
    date DATE NOT NULL,
    description TEXT NOT NULL,
    temp_min INTEGER NOT NULL,
    temp_max INTEGER NOT NULL
);
CREATE INDEX conditions_date_index ON conditions (date);
