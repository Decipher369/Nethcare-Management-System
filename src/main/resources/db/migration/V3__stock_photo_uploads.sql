-- Keep uploaded photos in persistent MySQL storage, separate from stock listing queries.
CREATE TABLE stock_images (
    name VARCHAR(64) NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    data LONGBLOB NOT NULL,
    PRIMARY KEY (name)
);
