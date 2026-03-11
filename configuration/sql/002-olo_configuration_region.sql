-- 002: Tenant ID → region mapping. Cached in Redis hash olo:worker:tenant:region (HSET/HGET).

DROP TABLE IF EXISTS olo_configuration_region;

CREATE TABLE olo_configuration_region (
    tenant_id  VARCHAR(64) PRIMARY KEY,
    region     VARCHAR(64) NOT NULL DEFAULT 'default',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_olo_configuration_region_region
ON olo_configuration_region(region);

INSERT INTO olo_configuration_region (tenant_id, region) VALUES
    ('tenant-a', 'default'),
    ('tenant-b', 'us-east'),
    ('tenant-c', 'eu-west');
