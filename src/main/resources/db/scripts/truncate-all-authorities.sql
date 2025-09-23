-- Delete records from the instance_authority_link table
TRUNCATE ${tenant_id}_mod_entities_links.instance_authority_link;

-- Delete records from the authority_data_stat table
TRUNCATE ${tenant_id}_mod_entities_links.authority_data_stat;

-- Delete records from the authority_archive table
TRUNCATE ${tenant_id}_mod_entities_links.authority_archive;

-- Delete records from the authority table
TRUNCATE ${tenant_id}_mod_entities_links.authority;

-- Delete associated records from the mod-source-record-storage marc_records_lb table
DELETE
FROM ${tenant_id}_mod_source_record_storage.marc_records_lb
    USING ${tenant_id}_mod_source_record_storage.records_lb
WHERE marc_records_lb.id = records_lb.id
  AND records_lb.record_type = 'MARC_AUTHORITY';

-- Delete associated records from the mod-source-record-storage records_lb table
DELETE
FROM ${tenant_id}_mod_source_record_storage.records_lb
WHERE record_type = 'MARC_AUTHORITY';