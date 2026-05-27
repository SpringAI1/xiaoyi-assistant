-- Initial Database Schema
-- Version: 1.0.0
-- Description: Core schema for enterprise knowledge system

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Documents table
CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_size BIGINT,
    file_path VARCHAR(1000),
    status VARCHAR(20) DEFAULT 'processing' CHECK (status IN ('processing', 'done', 'failed', 'deleted')),
    version INTEGER DEFAULT 1,
    uploaded_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Document metadata table (key-value pairs)
CREATE TABLE IF NOT EXISTS document_metadata (
    id BIGSERIAL PRIMARY KEY,
    document_id VARCHAR(64) REFERENCES documents(id) ON DELETE CASCADE,
    key VARCHAR(100) NOT NULL,
    value TEXT,
    UNIQUE(document_id, key)
);

-- Create index on documents for faster queries
CREATE INDEX IF NOT EXISTS idx_documents_filename ON documents(file_name);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents(created_at DESC);

-- Document chunks table
CREATE TABLE IF NOT EXISTS document_chunks (
    id VARCHAR(128) PRIMARY KEY,
    document_id VARCHAR(64) REFERENCES documents(id) ON DELETE CASCADE,
    chunk_number INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(384),
    word_count INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, chunk_number)
);

-- Create indexes for chunks
CREATE INDEX IF NOT EXISTS idx_chunks_document_id ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_chunks_embedding_cosine ON document_chunks USING hnsw(embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'user' CHECK (role IN ('admin', 'user', 'viewer')),
    email VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP WITH TIME ZONE
);

-- Create index on usernames
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Document permissions table
CREATE TABLE IF NOT EXISTS document_permissions (
    id VARCHAR(64) PRIMARY KEY,
    document_id VARCHAR(64) REFERENCES documents(id) ON DELETE CASCADE,
    user_id VARCHAR(64) REFERENCES users(id) ON DELETE CASCADE,
    permission_level VARCHAR(20) NOT NULL CHECK (permission_level IN ('read', 'write', 'admin')),
    UNIQUE(document_id, user_id)
);

-- Insert default admin user (password: admin123 hashed with BCrypt)
INSERT INTO users (id, username, password_hash, role, email)
VALUES ('admin-001', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iqtJqP8VohZTJqxKqTjBjHvMHqCi', 'ADMIN', 'admin@enterprise.local')
ON CONFLICT (username) DO NOTHING;
