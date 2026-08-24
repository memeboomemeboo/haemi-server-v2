WITH ordered_images AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY memory_id ORDER BY id) - 1 AS new_position
    FROM guardian_memory_images
)
UPDATE guardian_memory_images image
SET position = ordered_images.new_position
FROM ordered_images
WHERE image.id = ordered_images.id;
