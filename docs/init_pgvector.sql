\set ON_ERROR_STOP on

SELECT 'CREATE DATABASE speak_for_you_vector'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'speak_for_you_vector'
)\gexec

\connect speak_for_you_vector

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS speech_embeddings (
    embedding_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    embedding VECTOR(1024) NOT NULL,
    text TEXT NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

TRUNCATE TABLE speech_embeddings;

WITH personas(persona_type) AS (
    VALUES
        ('专业得体'),
        ('轻松友好'),
        ('委婉拒绝'),
        ('高情商化解'),
        ('简洁高效')
),
scenes(scene_type) AS (
    VALUES
        ('同事日常聊天'),
        ('领导/上级沟通'),
        ('客户/合作方沟通'),
        ('委婉拒绝/推脱')
),
combos AS (
    SELECT p.persona_type, s.scene_type
    FROM personas p
    CROSS JOIN scenes s
),
expanded AS (
    SELECT c.persona_type, c.scene_type, gs AS idx
    FROM combos c
    CROSS JOIN generate_series(1, 13) gs
),
limited AS (
    SELECT persona_type, scene_type, idx
    FROM expanded
    LIMIT 250
)
INSERT INTO speech_embeddings (embedding, text, metadata)
SELECT
    ('[' || array_to_string(array_fill(0.0::float4, ARRAY[1024]), ',') || ']')::vector AS embedding,
    format(
        '【%s-%s-%s】示例话术：已收到你的消息，我会尽快处理并及时给你反馈。',
        persona_type,
        scene_type,
        idx
    ) AS text,
    jsonb_build_object(
        'persona_type', persona_type,
        'scene_type', scene_type,
        'source', 'seed',
        'index', idx
    ) AS metadata
FROM limited;

SELECT COUNT(*) AS total_rows FROM speech_embeddings;
