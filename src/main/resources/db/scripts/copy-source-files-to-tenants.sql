-- =============================================================================
-- copy-source-files-to-tenants.sql
--
-- Copies authority_source_file and authority_source_file_code records from the
-- central tenant schema into each member tenant schema, ignoring conflicts.
--
-- Usage: copy this script into pgAdmin (or any SQL client), replace the
-- placeholders below, and execute.
--
-- Placeholders:
--   {centralTenant}  - tenant name whose schema is the data source
--                      (schema: <centralTenant>_mod_entities_links)
--   {memberTenants}  - comma-separated list of tenant names to copy data into
--                      (schema per tenant: <tenant>_mod_entities_links)
--
-- Example:
--   {centralTenant}  → consortium
--   {memberTenants}  → member1,member2
-- =============================================================================
DO $$
DECLARE
  central_schema TEXT;
  member_tenant  TEXT;
  member_schema  TEXT;
  member_tenants TEXT[];
  asf_count      INT;
  asfc_count     INT;
BEGIN
  -- Replace {centralTenant} with the actual central tenant name, e.g. 'consortium'
  central_schema := '{centralTenant}' || '_mod_entities_links';

  -- Replace {memberTenants} with a comma-separated list, e.g. 'member1,member2'
  member_tenants := string_to_array('{memberTenants}', ',');

  RAISE NOTICE 'Central schema: %', central_schema;
  RAISE NOTICE 'Member tenants: %', member_tenants;

  -- Verify that the central schema actually exists
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.schemata WHERE schema_name = central_schema
  ) THEN
    RAISE EXCEPTION 'Central schema "%" does not exist', central_schema;
  END IF;

  FOREACH member_tenant IN ARRAY member_tenants LOOP
    member_tenant := trim(member_tenant);
    member_schema := member_tenant || '_mod_entities_links';

    RAISE NOTICE '------------------------------------------------------------';
    RAISE NOTICE 'Processing member schema: %', member_schema;

    -- Verify that the member schema actually exists
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.schemata WHERE schema_name = member_schema
    ) THEN
      RAISE WARNING 'Member schema "%" does not exist – skipping', member_schema;
      CONTINUE;
    END IF;

    -- 1. Insert authority_source_file
    EXECUTE format(
      'INSERT INTO %I.authority_source_file
         (id, name, type, base_url, base_url_protocol, source,
          created_date, updated_date, created_by_user_id, updated_by_user_id,
          sequence_name, selectable, hrid_start_number, _version)
       SELECT
         id, name, type, base_url, base_url_protocol, source::text::%I.authority_source_file_source,
         created_date, updated_date, created_by_user_id, updated_by_user_id,
         sequence_name, selectable, hrid_start_number, _version
       FROM %I.authority_source_file
       ON CONFLICT DO NOTHING',
      member_schema, member_schema, central_schema
    );

    GET DIAGNOSTICS asf_count = ROW_COUNT;
    RAISE NOTICE 'authority_source_file      → inserted % row(s) into %', asf_count, member_schema;

    -- 2. Insert authority_source_file_code
    --    id is omitted – the sequence in each member schema generates it.
    EXECUTE format(
      'INSERT INTO %I.authority_source_file_code
         (authority_source_file_id, code)
       SELECT authority_source_file_id, code
       FROM %I.authority_source_file_code
       ON CONFLICT DO NOTHING',
      member_schema, central_schema
    );

    GET DIAGNOSTICS asfc_count = ROW_COUNT;
    RAISE NOTICE 'authority_source_file_code → inserted % row(s) into %', asfc_count, member_schema;

  END LOOP;

  RAISE NOTICE '============================================================';
  RAISE NOTICE 'Done.';
END;
$$;






