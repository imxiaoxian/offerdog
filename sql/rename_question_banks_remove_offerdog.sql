-- Remove "OfferDog " prefix from question bank names.
-- Safe to run multiple times.

BEGIN;

UPDATE question_banks
SET name = regexp_replace(name, '^OfferDog[ ]+', '')
WHERE deleted_at IS NULL
  AND name LIKE 'OfferDog %';

COMMIT;

