CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    default_persona_id BIGINT NULL,
    api_key VARCHAR(255) NULL,
    model_name VARCHAR(50) DEFAULT 'qwen-plus',
    role VARCHAR(20) DEFAULT 'USER',
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS persona (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    tone VARCHAR(500) NOT NULL,
    is_system BOOLEAN DEFAULT FALSE,
    user_id BIGINT NULL,
    created_at DATETIME
);

CREATE TABLE IF NOT EXISTS scene (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    prompt_hint VARCHAR(1000),
    is_system BOOLEAN DEFAULT TRUE,
    created_at DATETIME
);

CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    persona_id BIGINT NOT NULL,
    status TINYINT DEFAULT 1,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    sequence INT NOT NULL,
    created_at DATETIME
);

CREATE INDEX idx_chat_message_session_seq ON chat_message(session_id, sequence);
