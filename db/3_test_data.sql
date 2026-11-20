-- выполнить в базе lab_samples

INSERT INTO users (name, email) VALUES
    ('Иванов Иван', 'ivanov@mail.ru'),
    ('Петрова Анна', 'petrova@mail.ru'),
    ('Сидоров Олег', 'sidorov@mail.ru');

INSERT INTO analysis_methods (name, allowed_material, min_value, max_value, max_spread) VALUES
    ('Определение pH', 'вода', 0, 14, 0.5),
    ('Содержание нитратов', 'вода', 0, 100, 2),
    ('Влажность почвы', 'почва', 0, 100, 5);

INSERT INTO samples (material, customer_id, received_at, status) VALUES
    ('вода', 1, now() - interval '1 day', 'RECEIVED'),
    ('вода', 2, now() - interval '3 days', 'IN_WORK'),
    ('почва', 2, now() - interval '7 days', 'ANALYZED');

INSERT INTO analyses (sample_id, method_id, status, average_value, completed_at) VALUES
    (1, 1, 'NEW', NULL, NULL),
    (2, 1, 'IN_PROGRESS', 7.1, NULL),
    (3, 3, 'COMPLETED', 21.5, now() - interval '5 days');

INSERT INTO measurements (analysis_id, value, measured_at, confirmed) VALUES
    (2, 7.0, now() - interval '2 days', TRUE),
    (2, 7.2, now() - interval '2 days', TRUE),
    (2, 9.0, now() - interval '2 days', FALSE),
    (3, 21.0, now() - interval '6 days', TRUE),
    (3, 22.0, now() - interval '6 days', TRUE);
