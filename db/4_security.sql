-- выполнить под postgres в базе lab_samples
-- пароли: заказчики Customer1, laborant Laborant1, admin Admin1234

ALTER TABLE users ADD COLUMN username VARCHAR(30) UNIQUE;
ALTER TABLE users ADD COLUMN password VARCHAR(100);
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER' CHECK (role IN ('CUSTOMER', 'LAB', 'ADMIN'));
ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'BLOCKED'));

UPDATE users SET username = 'ivanov' WHERE email = 'ivanov@mail.ru';
UPDATE users SET username = 'petrova' WHERE email = 'petrova@mail.ru';
UPDATE users SET username = 'sidorov' WHERE email = 'sidorov@mail.ru';
UPDATE users SET username = 'user' || id WHERE username IS NULL;
UPDATE users SET password = '$2a$10$4A6qylBwliqTh2n6ZPP6feU3dvhZNl4mfS/VbgJLYCRAA.vTITOjS';

ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ALTER COLUMN password SET NOT NULL;

INSERT INTO users (username, password, name, email, role) VALUES
    ('laborant', '$2a$10$l3D2GaOHueQ0eSuQ7NJFXOGZUorkO.FBcrekQ2UBDtIerPiAKz5hW', 'Лаборант Ольга', 'lab@mail.ru', 'LAB'),
    ('admin', '$2a$10$.fmCT3tyGjnP2b6MFtf1h.tL7B//n73Y5mIDf4v6HvC8fp4pG1KcW', 'Администратор', 'admin@mail.ru', 'ADMIN');
