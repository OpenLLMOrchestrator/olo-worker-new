-- Idempotent schema and default data for bootstrap integration test.
-- Executed by BootstrapGlobalContextDumpTest when DB is configured.
-- Creates tables only if they do not exist; inserts default rows only when absent.
-- See: docs/architecture/bootstrap/how-to-debug.md

-- ---------------------------------------------------------------------------
-- 1. Tenant ID → region mapping (worker reads this; cached in Redis)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS olo_configuration_region (
    tenant_id  VARCHAR(64) PRIMARY KEY,
    region     VARCHAR(64) NOT NULL DEFAULT 'default',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_olo_configuration_region_region
ON olo_configuration_region(region);

INSERT INTO olo_configuration_region (tenant_id, region) VALUES
    ('tenant-a', 'default'),
    ('tenant-b', 'us-east'),
    ('tenant-c', 'eu-west')
ON CONFLICT (tenant_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Resource-scoped configuration (snapshot source for DB → Redis)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS olo_config_resource (
    resource_id   VARCHAR(255) NOT NULL,
    tenant_id     VARCHAR(64) NOT NULL DEFAULT '',
    region        VARCHAR(64) NOT NULL DEFAULT 'default',
    config_json   TEXT NOT NULL,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (resource_id, tenant_id, region)
);

-- Sample config precedence for the same logical config key:
-- global (per-region) → regional (different region snapshot) → tenant-specific.
INSERT INTO olo_config_resource (resource_id, tenant_id, region, config_json) VALUES
    -- Region=default, global defaults
    ('core:sample', '', 'default', '{
      "app": { "theme": "light", "timeoutSecs": 30 },
      "feature": { "beta": false }
    }'),
    -- Region=us-east, regional defaults (for all tenants in us-east)
    ('core:sample', '', 'us-east', '{
      "app": { "theme": "dark", "timeoutSecs": 25 },
      "feature": { "beta": true }
    }'),
    -- Region=us-east, tenant-specific override for tenant-a
    ('core:sample', 'tenant-a', 'us-east', '{
      "app": { "timeoutSecs": 10 }
    }'),
    -- Minimal example used in docs: region id only
    ('olo.region', '', 'default', '{"olo.region":"default"}')
ON CONFLICT (resource_id, tenant_id, region) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3. Region pipeline template (005) + tenant override (006)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS olo_pipeline_template (
    region      VARCHAR(64)  NOT NULL,
    pipeline_id VARCHAR(255) NOT NULL,
    version     BIGINT       NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT true,
    tree_json   JSONB        NOT NULL,
    checksum    VARCHAR(64),
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (region, pipeline_id, version)
);

CREATE INDEX IF NOT EXISTS idx_olo_pipeline_template_region
ON olo_pipeline_template(region);

CREATE INDEX IF NOT EXISTS idx_olo_pipeline_template_region_pipeline
ON olo_pipeline_template(region, pipeline_id);

CREATE INDEX IF NOT EXISTS idx_olo_pipeline_template_active
ON olo_pipeline_template(region, pipeline_id, is_active);

CREATE TABLE IF NOT EXISTS olo_tenant_pipeline_override (
    tenant_id        VARCHAR(64)  NOT NULL,
    pipeline_id      VARCHAR(255) NOT NULL,
    pipeline_version BIGINT       NOT NULL,
    override_json    JSONB        NOT NULL,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, pipeline_id, pipeline_version)
);

CREATE INDEX IF NOT EXISTS idx_olo_tenant_pipeline_override_tenant
ON olo_tenant_pipeline_override(tenant_id);

CREATE INDEX IF NOT EXISTS idx_olo_tenant_pipeline_override_pipeline
ON olo_tenant_pipeline_override(pipeline_id, pipeline_version);

-- Default region: default-pipeline (bootstrap default)
INSERT INTO olo_pipeline_template (region, pipeline_id, version, is_active, tree_json, updated_at)
VALUES (
  'default',
  'default-pipeline',
  1,
  true,
  '{"id":"default-pipeline","name":"default-pipeline","version":1,"workflowId":"default","description":"Bootstrap default pipeline","inputContract":{"strict":false,"parameters":[]},"variableRegistry":[],"scope":{"plugins":[],"features":[]},"executionTree":{"id":"root","displayName":"Pipeline","type":"SEQUENCE","children":[]},"outputContract":{"parameters":[]},"resultMapping":[]}'::jsonb,
  CURRENT_TIMESTAMP
)
ON CONFLICT (region, pipeline_id, version) DO NOTHING;
