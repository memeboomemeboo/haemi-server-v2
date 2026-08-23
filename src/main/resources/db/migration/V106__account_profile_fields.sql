ALTER TABLE accounts
    ADD COLUMN gender VARCHAR(20),
    ADD COLUMN profile_image_url VARCHAR(500);

ALTER TABLE guardian_families
    ADD COLUMN memo VARCHAR(30),
    ADD COLUMN profile_image_url VARCHAR(500);
