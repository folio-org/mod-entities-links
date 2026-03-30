-- ==========================================================
-- DESCRIPTION:
--   This SQL script contains procedures to clean up propagated
--   authority data for a consortium environment. It handles:
--     - Member tenants propagated data cleanup
--       (authority, authority_archive, instance_authority_link)
--   Uses UUID batch ranges for efficient deletion.
-- ==========================================================

-- ==========================================================
-- PROCEDURE: cleanup_member_tenant_propagated_data
-- DESCRIPTION:
--   Cleans up data in a consortium member tenant (e.g., college, university)
--   propagated from the consortium central tenant.
--   Deletes:
--     - authority rows with source = 'CONSORTIUM-MARC'
--     - instance_authority_link rows for shared MARC bibliographic records
--   Uses UUID ranges to batch deletions.
-- ==========================================================
CREATE OR REPLACE PROCEDURE cleanup_member_tenant_propagated_data(member_tenant TEXT)
LANGUAGE plpgsql
AS $$
DECLARE
    arr UUID[] := ARRAY[
        '10000000-0000-0000-0000-000000000000',
        '20000000-0000-0000-0000-000000000000',
        '30000000-0000-0000-0000-000000000000',
        '40000000-0000-0000-0000-000000000000',
        '50000000-0000-0000-0000-000000000000',
        '60000000-0000-0000-0000-000000000000',
        '70000000-0000-0000-0000-000000000000',
        '80000000-0000-0000-0000-000000000000',
        '90000000-0000-0000-0000-000000000000',
        'a0000000-0000-0000-0000-000000000000',
        'b0000000-0000-0000-0000-000000000000',
        'c0000000-0000-0000-0000-000000000000',
        'd0000000-0000-0000-0000-000000000000',
        'e0000000-0000-0000-0000-000000000000',
        'f0000000-0000-0000-0000-000000000000',
        'ffffffff-ffff-ffff-ffff-ffffffffffff'
    ];

    lower UUID := '00000000-0000-0000-0000-000000000000'; -- lower bound of UUID batch
    upper UUID; -- upper bound of UUID batch
BEGIN
    RAISE INFO '========== STARTING MEMBER TENANT: % ==========', member_tenant;

    BEGIN
        FOREACH upper IN ARRAY arr LOOP
            -- Delete shadow copies from the authority
            EXECUTE format(
                'DELETE FROM %I_mod_entities_links.authority
                 WHERE source = ''CONSORTIUM-MARC''
                   AND id > $1 AND id <= $2',
                member_tenant
            ) USING lower, upper;

            -- Delete links for shared MARC bib records
            EXECUTE format(
                'DELETE FROM %I_mod_entities_links.instance_authority_link ial
                 WHERE ial.authority_id > $1
                   AND ial.authority_id <= $2
                   AND NOT EXISTS (
                       SELECT 1
                       FROM %I_mod_inventory_storage.instance i
                       WHERE i.id = ial.instance_id
                         AND i.jsonb->>''source'' IS DISTINCT FROM ''CONSORTIUM-MARC''
                   );',
                member_tenant, member_tenant
            ) USING lower, upper;

            -- move lower bound for next batch
            lower := upper;
        END LOOP;
    EXCEPTION WHEN OTHERS THEN
        RAISE WARNING 'Member Tenant % FAILED: %', member_tenant, SQLERRM;
    END;
    COMMIT;
    RAISE INFO '========== FINISHED MEMBER TENANT: % ==========', member_tenant;
END;
$$;

-- ==========================================================
-- PROCEDURE: cleanup_all_member_tenants
-- DESCRIPTION:
--   Runs cleanup for all consortium member tenants
--   IMPORTANT:
--     - The following values are provided as an example:
--           member_tenants TEXT[] := ARRAY['college', 'university'];
--     - Need to set the appropriate consortium member tenants names in the
--       array for the environment. These values should match the schema/table prefixes.
-- ==========================================================
CREATE OR REPLACE PROCEDURE cleanup_all_member_tenants()
LANGUAGE plpgsql
AS $$
DECLARE
    member_tenants TEXT[] := ARRAY['college', 'university']; -- example list of consortium member tenants
    member_tenant TEXT;
BEGIN
    -- Loop through consortium member tenants and cleanup propagated data
    FOREACH member_tenant IN ARRAY member_tenants LOOP
        CALL cleanup_member_tenant_propagated_data(member_tenant);
    END LOOP;
END;
$$;

-- ==========================================================
-- USAGE:
--   -- Cleanup all member tenants
--   CALL cleanup_all_member_tenants();
--
--   -- Cleanup only one member tenant
--   CALL cleanup_member_tenant_propagated_data('college');
-- ==========================================================

-- Drop procedures after use
DROP PROCEDURE IF EXISTS cleanup_member_tenant_propagated_data(TEXT);
DROP PROCEDURE IF EXISTS cleanup_all_member_tenants();
