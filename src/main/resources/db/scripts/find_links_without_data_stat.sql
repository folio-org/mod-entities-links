-- Find instance_authority_link records whose authority has been updated
-- (created_date != updated_date) but has no corresponding authority_data_stat entry created after the authority update.
-- The data_stat creation date check (started_at > updated_date) ensures coverage of authorities updated multiple times,
-- verifying that a stat exists specifically for the latest update.
-- This covers the case when authority kafka consumer was down resulting in outdated data in bibs linked to authorities.
SELECT
    ial.id          AS link_id,
    a.id            AS authority_id,
    ads.id          AS data_stat_id
FROM ${tenant_id}_mod_entities_links.instance_authority_link ial
         INNER JOIN ${tenant_id}_mod_entities_links.authority a
                    ON a.id = ial.authority_id
                        AND a.created_date <> a.updated_date
         LEFT JOIN ${tenant_id}_mod_entities_links.authority_data_stat ads
                   ON ads.authority_id = a.id
                       AND ads.started_at > a.updated_date
WHERE ads.id IS NULL;

