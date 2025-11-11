-- Seed three example packages
INSERT INTO public.travel_packages (id, name, location, base_price, overview, location_points, timing)
VALUES
    ('00000000-0000-0000-0000-0000000000a1', 'Cox''s Bazar Getaway', 'Cox''s Bazar', 15999.00,
     'Three-day beach retreat with sea views. Placeholder overview.',
     'Kolatoli Beach, Marine Drive, Himchari National Park, Mermaid Beach Resort',
     '3 days, 2 to 6 people'),
    ('00000000-0000-0000-0000-0000000000b2', 'Bandarban Hills Escape', 'Bandarban', 12999.00,
     'Explore the green hills and tribal culture. Placeholder overview.',
     'Nilgiri, Nilachal, Meghla, Shoilo Propat',
     '3 days, 2 to 6 people'),
    ('00000000-0000-0000-0000-0000000000c3', 'Sajek Valley Serenity', 'Sajek', 14999.00,
     'Clouds and valleys in a calm retreat. Placeholder overview.',
     'Risang, Konglak Para, Helipad, Ruilui Para',
     '3 days, 2 to 6 people')
ON CONFLICT DO NOTHING;

