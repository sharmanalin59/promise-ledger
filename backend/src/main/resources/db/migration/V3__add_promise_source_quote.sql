-- Verbatim transcript excerpt a promise was extracted from (provenance).
-- Distinct from evidence, which holds manually entered verification material.
-- Nullable so existing promises remain valid.
ALTER TABLE promises
    ADD COLUMN source_quote TEXT NULL AFTER statement;
