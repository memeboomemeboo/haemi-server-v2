UPDATE guardian_elder_links SET role = 'GUARDIAN'      WHERE role = '보호자';
UPDATE guardian_elder_links SET role = 'DAUGHTER'      WHERE role = '딸';
UPDATE guardian_elder_links SET role = 'SON'           WHERE role = '아들';
UPDATE guardian_elder_links SET role = 'GRANDDAUGHTER' WHERE role = '손녀';
UPDATE guardian_elder_links SET role = 'GRANDSON'      WHERE role = '손자';
UPDATE guardian_elder_links SET role = 'OTHER'         WHERE role = '기타';

ALTER TABLE guardian_elder_links ALTER COLUMN role SET DEFAULT 'GUARDIAN';
