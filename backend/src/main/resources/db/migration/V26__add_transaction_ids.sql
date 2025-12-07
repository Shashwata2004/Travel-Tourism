-- Add transaction_id (6-digit with TXN- prefix) and card_last4 to bookings and hotel_room_bookings.

ALTER TABLE hotel_room_bookings ADD COLUMN transaction_id varchar(16);
ALTER TABLE hotel_room_bookings ADD COLUMN card_last4 varchar(4);

WITH numbered AS (
    SELECT id,
           'TXN-' || to_char(row_number() OVER (ORDER BY COALESCE(created_at, now()), id) + 100000, 'FM000000') AS tid,
           lpad((floor(random() * 10000))::text, 4, '0') AS last4
    FROM hotel_room_bookings
)
UPDATE hotel_room_bookings h
SET transaction_id = COALESCE(h.transaction_id, n.tid),
    card_last4 = COALESCE(h.card_last4, n.last4)
FROM numbered n
WHERE h.id = n.id;

ALTER TABLE hotel_room_bookings ALTER COLUMN transaction_id SET NOT NULL;
ALTER TABLE hotel_room_bookings ADD CONSTRAINT uq_hotel_room_bookings_txn UNIQUE (transaction_id);

ALTER TABLE bookings ADD COLUMN transaction_id varchar(16);
ALTER TABLE bookings ADD COLUMN card_last4 varchar(4);

WITH numbered_pkg AS (
    SELECT id,
           'TXN-' || to_char(row_number() OVER (ORDER BY COALESCE(created_at, now()), id) + 200000, 'FM000000') AS tid,
           lpad((floor(random() * 10000))::text, 4, '0') AS last4
    FROM bookings
)
UPDATE bookings b
SET transaction_id = COALESCE(b.transaction_id, n.tid),
    card_last4 = COALESCE(b.card_last4, n.last4)
FROM numbered_pkg n
WHERE b.id = n.id;

ALTER TABLE bookings ALTER COLUMN transaction_id SET NOT NULL;
ALTER TABLE bookings ADD CONSTRAINT uq_bookings_txn UNIQUE (transaction_id);
