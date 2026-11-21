ALTER TABLE app_user
    ADD COLUMN role varchar(16) NOT NULL DEFAULT 'USER'
        CHECK (role IN ('USER', 'ADMIN'));
