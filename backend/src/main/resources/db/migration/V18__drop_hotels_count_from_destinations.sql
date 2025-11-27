-- Drop obsolete hotels_count column; counts are computed from hotels table
ALTER TABLE travel_destinations DROP COLUMN IF EXISTS hotels_count;
