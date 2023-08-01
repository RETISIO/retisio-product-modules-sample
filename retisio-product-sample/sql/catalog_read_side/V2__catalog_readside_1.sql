DROP TABLE IF EXISTS CATALOG;

CREATE TABLE IF NOT EXISTS CATALOG
(
    CATALOG_ID VARCHAR(255) NOT NULL,
    CATALOG_NAME VARCHAR(500) NOT NULL,
    IS_ACTIVE BOOLEAN DEFAULT FALSE,
    DYNA_PROPS TEXT,
    IS_DELETED BOOLEAN DEFAULT FALSE,
    CREATED_TMST TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    LAST_MODIFIED_TMST TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(CATALOG_ID)
);