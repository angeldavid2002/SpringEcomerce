-- Marcar si el email del usuario ha sido verificado
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
