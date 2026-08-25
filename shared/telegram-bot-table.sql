-- AzərKalori Telegram bot üçün sadə cədvəl.
-- DataGrip və ya psql-də azerkalori DB-də bir dəfə işlət.
CREATE TABLE IF NOT EXISTS public.telegram_food_logs (
    id         BIGSERIAL PRIMARY KEY,
    chat_id    BIGINT       NOT NULL,
    food       TEXT         NOT NULL,
    calories   NUMERIC      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_tg_logs_chat_date
    ON public.telegram_food_logs (chat_id, created_at);
