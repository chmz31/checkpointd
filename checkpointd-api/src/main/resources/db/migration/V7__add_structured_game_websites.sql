ALTER TABLE game_websites
    ADD COLUMN IF NOT EXISTS website_label VARCHAR(255),
    ADD COLUMN IF NOT EXISTS trusted BOOLEAN DEFAULT FALSE;

UPDATE game_websites
SET website_label = CASE
    WHEN website_url ILIKE '%steampowered.com%' THEN 'Steam'
    WHEN website_url ILIKE '%gog.com%' THEN 'GOG'
    WHEN website_url ILIKE '%epicgames.com%' THEN 'Epic Games'
    WHEN website_url ILIKE '%playstation.com%' THEN 'PlayStation'
    WHEN website_url ILIKE '%xbox.com%' THEN 'Xbox'
    WHEN website_url ILIKE '%nintendo.com%' THEN 'Nintendo'
    WHEN website_url ILIKE '%youtube.com%' OR website_url ILIKE '%youtu.be%' THEN 'YouTube'
    WHEN website_url ILIKE '%twitch.tv%' THEN 'Twitch'
    WHEN website_url ILIKE '%discord.gg%' OR website_url ILIKE '%discord.com%' THEN 'Discord'
    WHEN website_url ILIKE '%reddit.com%' THEN 'Reddit'
    WHEN website_url ILIKE '%wikipedia.org%' THEN 'Wikipedia'
    ELSE COALESCE(
        NULLIF(
            INITCAP(SPLIT_PART(
                REGEXP_REPLACE(
                    REGEXP_REPLACE(website_url, '^https?://(www\.)?', '', 'i'),
                    '/.*$',
                    ''
                ),
                '.',
                1
            )),
            ''
        ),
        'Website'
    )
END
WHERE website_label IS NULL OR BTRIM(website_label) = '';

UPDATE game_websites
SET trusted = FALSE
WHERE trusted IS NULL;

ALTER TABLE game_websites
    ALTER COLUMN website_label SET NOT NULL,
    ALTER COLUMN trusted SET DEFAULT FALSE,
    ALTER COLUMN trusted SET NOT NULL;
