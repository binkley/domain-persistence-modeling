SELECT table_version.ver_enable_versioning('public', 'other');
SELECT table_version.ver_enable_versioning('public', 'parent');
SELECT table_version.ver_enable_versioning('public', 'child');

SELECT table_version.ver_create_revision('Init');
SELECT table_version.ver_complete_revision();
