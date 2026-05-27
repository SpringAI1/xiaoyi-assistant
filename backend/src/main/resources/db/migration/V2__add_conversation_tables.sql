-- Add Conversation and Chat Message Tables
-- Version: 2.0.0
-- Description: Add tables for conversation persistence

-- Conversations table
CREATE TABLE IF NOT EXISTS conversations (
    session_id VARCHAR(128) PRIMARY KEY,
    user_id VARCHAR(64),
    title VARCHAR(500),
    summary TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    message_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE
);

-- Create indexes for conversations
CREATE INDEX IF NOT EXISTS idx_conversations_user_id ON conversations(user_id);
CREATE INDEX IF NOT EXISTS idx_conversations_updated_at ON conversations(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_conversations_expires_at ON conversations(expires_at);

-- Chat messages table
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(128) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    message_order INTEGER
);

-- Create indexes for chat messages
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_id ON chat_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_order ON chat_messages(session_id, message_order);
