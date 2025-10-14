ALTER TABLE execution_report ADD COLUMN symbol VARCHAR(32) NOT NULL;
-- Pense à retirer le DEFAULT après migration si tu veux imposer la présence du symbole à la création

