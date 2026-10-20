-- Demo data for local exploration. Run after the app has started once (Hibernate creates the schema):
--   docker compose exec -T db psql -U cinema -d absolutecinema < scripts/demo-data.sql
-- All demo accounts use the password: demo1234

INSERT INTO users (id, username, password, email, first_name, last_name, acctyp, points) VALUES
  (1, 'admin',    '$2a$10$Il1EU8cymFbKaGZiFH21reZj2LHAAzPy.py6gJGJCf1JOjMZVDRI2', 'admin@absolutecinema.dev',    'Ada',   'Admin',    'admin',    0),
  (2, 'employee', '$2a$10$Il1EU8cymFbKaGZiFH21reZj2LHAAzPy.py6gJGJCf1JOjMZVDRI2', 'employee@absolutecinema.dev', 'Evan',  'Employee', 'employee', 0),
  (3, 'demo',     '$2a$10$Il1EU8cymFbKaGZiFH21reZj2LHAAzPy.py6gJGJCf1JOjMZVDRI2', 'demo@absolutecinema.dev',     'Dana',  'Demo',     'user',     640)
ON CONFLICT DO NOTHING;

INSERT INTO movies (id, title, genre, director, description, duration, language, releasedate, ticketssold) VALUES
  (1, 'Dune: Part Two',   'Sci-Fi',    'Denis Villeneuve',   'Paul Atreides unites with the Fremen while on a path of revenge against the conspirators who destroyed his family.', 166, 'English', '2024-03-01', 412),
  (2, 'Oppenheimer',      'Drama',     'Christopher Nolan',  'The story of J. Robert Oppenheimer and his role in the development of the atomic bomb.',                             180, 'English', '2023-07-21', 365),
  (3, 'Spider-Man: Across the Spider-Verse', 'Animation', 'Joaquim Dos Santos', 'Miles Morales catapults across the Multiverse and encounters a team of Spider-People.',            140, 'English', '2023-06-02', 298),
  (4, 'The Batman',       'Action',    'Matt Reeves',        'Batman ventures into Gotham City''s underworld when a sadistic killer leaves behind a trail of cryptic clues.',   176, 'English', '2022-03-04', 251),
  (5, 'Inside Out 2',     'Animation', 'Kelsey Mann',        'Riley enters her teenage years and Headquarters is suddenly demolished to make room for new Emotions.',         96,  'English', '2024-06-14', 334),
  (6, 'Parasite',         'Thriller',  'Bong Joon-ho',       'Greed and class discrimination threaten the newly formed relationship between the wealthy Park family and the destitute Kim clan.', 132, 'Korean', '2019-05-30', 187)
ON CONFLICT DO NOTHING;

INSERT INTO sessions (id, movie_id, session_time, available_seats)
SELECT m.id * 10 + s.n, m.id, date_trunc('day', now()) + interval '1 day' * s.n + interval '13 hours' + interval '3 hours' * (m.id % 3), 80 - (m.id * 7 + s.n * 5) % 40
FROM movies m CROSS JOIN generate_series(0, 2) AS s(n)
ON CONFLICT DO NOTHING;

INSERT INTO snacks (id, name, description, price, quantity, snackssold) VALUES
  (1, 'Classic Butter Popcorn', 'Freshly popped with real butter',   5.0, 120, 340),
  (2, 'Caramel Popcorn',        'Sweet, crunchy caramel coating',      6.0, 80,  210),
  (3, 'Cheese Nachos',          'Tortilla chips with warm cheese dip', 6.5, 60,  175),
  (4, 'Coca Cola',              'Chilled 500ml bottle',                3.0, 200, 420),
  (5, 'Espresso',               'Double shot, freshly brewed',         3.5, 90,  95),
  (6, 'Fresh Orange Juice',     'Cold-pressed oranges',                4.0, 50,  88)
ON CONFLICT DO NOTHING;

INSERT INTO bookings (id, user_id, movie_id, session_id, number_of_tickets, total_price, booking_time) VALUES
  (1, 3, 1, 10, 2, 24.0, now() - interval '20 days'),
  (2, 3, 2, 20, 1, 12.0, now() - interval '12 days'),
  (3, 3, 5, 50, 3, 41.0, now() - interval '3 days')
ON CONFLICT DO NOTHING;

INSERT INTO feedback (id, user_id, movie_id, rating, comments, createdat) VALUES
  (1, 3, 1, 5, 'Stunning visuals and sound. Best IMAX experience I have had.', now() - interval '19 days'),
  (2, 3, 2, 4, 'Long but gripping. The seats were comfortable the whole way.', now() - interval '11 days'),
  (3, 3, 5, 5, 'Brought the whole family, everyone loved it.',                 now() - interval '2 days')
ON CONFLICT DO NOTHING;

INSERT INTO employees (id, firstname, lastname, email, phonenumber, address, department, position, salary, hiredate, active) VALUES
  (1, 'Evan',  'Employee', 'employee@absolutecinema.dev', '+92 300 0000001', 'Lahore', 'Operations', 'Shift Supervisor', 1800, '2024-01-15', true),
  (2, 'Sara',  'Khan',     'sara@absolutecinema.dev',     '+92 300 0000002', 'Lahore', 'Concessions', 'Cashier',         1200, '2024-05-02', true),
  (3, 'Bilal', 'Ahmed',    'bilal@absolutecinema.dev',    '+92 300 0000003', 'Lahore', 'Projection',  'Projectionist',   1500, '2023-11-20', true)
ON CONFLICT DO NOTHING;

-- Keep identity sequences ahead of the seeded ids
SELECT setval(pg_get_serial_sequence(t, 'id'), 1000)
FROM unnest(ARRAY['movies','sessions','snacks','bookings','feedback','employees']) AS t
WHERE pg_get_serial_sequence(t, 'id') IS NOT NULL;
