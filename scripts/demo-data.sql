-- Demo data for local exploration. Run after the app has started once (Hibernate creates the schema):
--   docker compose exec -T db psql -U cinema -d absolutecinema < scripts/demo-data.sql
-- All demo accounts use the password: demo1234

INSERT INTO users (id, username, password, email, first_name, last_name, acctyp, points) VALUES
  (1, 'admin',    '$2a$10$Il1EU8cymFbKaGZiFH21reZj2LHAAzPy.py6gJGJCf1JOjMZVDRI2', 'admin@absolutecinema.dev',    'Ada',   'Admin',    'admin',    0),
  (2, 'employee', '$2a$10$Il1EU8cymFbKaGZiFH21reZj2LHAAzPy.py6gJGJCf1JOjMZVDRI2', 'employee@absolutecinema.dev', 'Evan',  'Employee', 'employee', 0),
  (3, 'demo',     '$2a$10$Il1EU8cymFbKaGZiFH21reZj2LHAAzPy.py6gJGJCf1JOjMZVDRI2', 'demo@absolutecinema.dev',     'Dana',  'Demo',     'user',     640),
  (4, 'aisha',    '$2a$10$Il1EU8cymFbKaGZiFH21reZj2LHAAzPy.py6gJGJCf1JOjMZVDRI2', 'aisha@absolutecinema.dev',    'Aisha', 'Malik',    'user',     1240),
  (5, 'omar',     '$2a$10$Il1EU8cymFbKaGZiFH21reZj2LHAAzPy.py6gJGJCf1JOjMZVDRI2', 'omar@absolutecinema.dev',     'Omar',  'Farooq',   'user',     310),
  (6, 'hina',     '$2a$10$Il1EU8cymFbKaGZiFH21reZj2LHAAzPy.py6gJGJCf1JOjMZVDRI2', 'hina@absolutecinema.dev',     'Hina',  'Raza',     'user',     90)
ON CONFLICT DO NOTHING;

INSERT INTO movies (id, title, genre, director, description, duration, language, releasedate, ticketssold, poster_url) VALUES
  (1, 'Dune: Part Two',   'Sci-Fi',    'Denis Villeneuve',   'Paul Atreides unites with the Fremen while on a path of revenge against the conspirators who destroyed his family.', 166, 'English', '2024-03-01', 412, 'https://upload.wikimedia.org/wikipedia/en/5/52/Dune_Part_Two_poster.jpeg'),
  (2, 'Oppenheimer',      'Drama',     'Christopher Nolan',  'The story of J. Robert Oppenheimer and his role in the development of the atomic bomb.',                             180, 'English', '2023-07-21', 365, 'https://upload.wikimedia.org/wikipedia/en/4/4a/Oppenheimer_%28film%29.jpg'),
  (3, 'Spider-Man: Across the Spider-Verse', 'Animation', 'Joaquim Dos Santos', 'Miles Morales catapults across the Multiverse and encounters a team of Spider-People.',            140, 'English', '2023-06-02', 298, 'https://upload.wikimedia.org/wikipedia/en/b/b4/Spider-Man-_Across_the_Spider-Verse_poster.jpg'),
  (4, 'The Batman',       'Action',    'Matt Reeves',        'Batman ventures into Gotham City''s underworld when a sadistic killer leaves behind a trail of cryptic clues.',   176, 'English', '2022-03-04', 251, 'https://upload.wikimedia.org/wikipedia/en/f/ff/The_Batman_%28film%29_poster.jpg'),
  (5, 'Inside Out 2',     'Animation', 'Kelsey Mann',        'Riley enters her teenage years and Headquarters is suddenly demolished to make room for new Emotions.',         96,  'English', '2024-06-14', 334, 'https://upload.wikimedia.org/wikipedia/en/f/f7/Inside_Out_2_poster.jpg'),
  (6, 'Parasite',         'Thriller',  'Bong Joon-ho',       'Greed and class discrimination threaten the newly formed relationship between the wealthy Park family and the destitute Kim clan.', 132, 'Korean', '2019-05-30', 187, 'https://upload.wikimedia.org/wikipedia/en/5/53/Parasite_%282019_film%29.png')
ON CONFLICT DO NOTHING;

INSERT INTO sessions (id, movie_id, session_time, available_seats)
SELECT m.id * 10 + s.n, m.id, date_trunc('day', now()) + interval '1 day' * s.n + interval '13 hours' + interval '3 hours' * (m.id % 3), 80 - (m.id * 7 + s.n * 5) % 40
FROM movies m CROSS JOIN generate_series(0, 2) AS s(n)
WHERE m.id <= 6
ON CONFLICT DO NOTHING;

INSERT INTO snacks (id, name, description, price, quantity, snackssold, image_url) VALUES
  (1, 'Classic Butter Popcorn', 'Freshly popped with real butter',  5.0, 120, 340, '/images/snacks/butter-popcorn.jpg'),
  (2, 'Caramel Popcorn',        'Sweet, crunchy caramel coating',     6.0, 80,  210, '/images/snacks/caramel-popcorn.jpg'),
  (3, 'Cheese Popcorn',         'Coated in aged cheddar',             6.5, 60,  175, '/images/snacks/cheese-popcorn.jpg'),
  (4, 'Coca Cola',              'Chilled 500ml bottle',               3.0, 200, 420, '/images/snacks/coca-cola.jpg'),
  (5, 'Sparkling Water',        'Lightly carbonated mineral water',   2.5, 150, 120, '/images/snacks/sparkling-water.jpg'),
  (6, 'Fresh Orange Juice',     'Cold-pressed oranges',               4.0, 50,  88,  '/images/snacks/orange-juice.jpg'),
  (7, 'Espresso',               'Double shot, freshly brewed',        3.5, 90,  95,  '/images/snacks/espresso.jpg'),
  (8, 'Cappuccino',             'Espresso with steamed milk foam',    4.5, 90,  110, '/images/snacks/cappuccino.jpg')
ON CONFLICT DO NOTHING;

-- Bookings spread over the last six months so the analytics charts have history
INSERT INTO bookings (id, user_id, movie_id, session_id, number_of_tickets, total_price, booking_time)
SELECT b.n,
       3 + b.n % 4,
       1 + b.n % 6,
       (1 + b.n % 6) * 10,
       1 + b.n % 3,
       (1 + b.n % 3) * 12.0 + CASE WHEN b.n % 2 = 0 THEN 11.0 ELSE 0 END,
       date_trunc('month', now()) - interval '1 month' * (b.n % 5) + interval '1 day' * (b.n % 19) + interval '19 hours'
FROM generate_series(1, 36) AS b(n)
ON CONFLICT DO NOTHING;

-- Even-numbered bookings include a butter and a caramel popcorn (5.0 + 6.0 = 11.0 of the total above)
INSERT INTO booking_snack (id, bookingid, snackid, quantity)
SELECT b.n * 2 - 2 + s.snack, b.n, s.snack, 1
FROM generate_series(2, 36, 2) AS b(n) CROSS JOIN (VALUES (1), (2)) AS s(snack)
ON CONFLICT DO NOTHING;

INSERT INTO feedback (id, user_id, movie_id, rating, comments, createdat) VALUES
  (1, 3, 1, 5, 'Stunning visuals and sound. Best IMAX experience I have had.', now() - interval '19 days'),
  (2, 3, 2, 4, 'Long but gripping. The seats were comfortable the whole way.', now() - interval '11 days'),
  (3, 3, 5, 5, 'Brought the whole family, everyone loved it.',                 now() - interval '2 days'),
  (4, 4, 6, 5, 'Still holds up. Great to see it on the big screen again.',     now() - interval '6 days'),
  (5, 5, 4, 3, 'Good film, but the hall was too cold.',                        now() - interval '4 days'),
  (6, 6, 3, 4, 'The animation is unreal. Snacks queue was slow though.',      now() - interval '1 day')
ON CONFLICT DO NOTHING;

INSERT INTO employees (id, firstname, lastname, email, phonenumber, address, department, position, salary, hiredate, active) VALUES
  (1, 'Evan',  'Employee', 'employee@absolutecinema.dev', '+92 300 0000001', 'Lahore', 'Operations',  'Shift Supervisor', 85000, '2024-01-15', true),
  (2, 'Sara',  'Khan',     'sara@absolutecinema.dev',     '+92 300 0000002', 'Lahore', 'Concessions', 'Cashier',          55000, '2024-05-02', true),
  (3, 'Bilal', 'Ahmed',    'bilal@absolutecinema.dev',    '+92 300 0000003', 'Lahore', 'Projection',  'Projectionist',    65000, '2023-11-20', true)
ON CONFLICT DO NOTHING;

-- This month's shifts for the demo employee
INSERT INTO shifts (id, employee_id, starttime, endtime, hours)
SELECT d.n, 1,
       date_trunc('month', now()) + interval '1 day' * (d.n - 1) * 2 + interval '14 hours',
       date_trunc('month', now()) + interval '1 day' * (d.n - 1) * 2 + interval '22 hours',
       8.0
FROM generate_series(1, 5) AS d(n)
ON CONFLICT DO NOTHING;

-- Keep identity sequences ahead of the seeded ids
SELECT setval(pg_get_serial_sequence(t, 'id'), 1000)
FROM unnest(ARRAY['movies','sessions','snacks','bookings','booking_snack','feedback','employees','shifts']) AS t
WHERE pg_get_serial_sequence(t, 'id') IS NOT NULL;
