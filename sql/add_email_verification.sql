-- Script SQL para crear tabla de códigos de verificación
-- Ejecutar en PostgreSQL

CREATE TABLE email_verification_codes (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    codigo VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Crear índices para optimizar consultas
CREATE INDEX idx_email_codigo ON email_verification_codes(email, codigo);
CREATE INDEX idx_expires_at ON email_verification_codes(expires_at);
CREATE INDEX idx_email_used ON email_verification_codes(email, used);
