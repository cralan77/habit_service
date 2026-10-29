-- V1__init_habit_tables.sql

-- 1. Configurações de Rotina do Usuário
CREATE TABLE IF NOT EXISTS user_routine_settings (
    user_id VARCHAR(128) PRIMARY KEY,
    week_start_day VARCHAR(16) NOT NULL DEFAULT 'MONDAY', -- MONDAY, SUNDAY, SATURDAY...
    timezone VARCHAR(64) NOT NULL DEFAULT 'America/Sao_Paulo',
    evening_reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    evening_reminder_time VARCHAR(8) NOT NULL DEFAULT '20:00',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Categorias da To-Do List
CREATE TABLE IF NOT EXISTS todo_categories (
    id UUID PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    name VARCHAR(100) NOT NULL,
    color_hex VARCHAR(16) NOT NULL DEFAULT '#6366F1',
    icon VARCHAR(50) NOT NULL DEFAULT 'folder',
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_todo_category_user_name UNIQUE (user_id, name)
);
CREATE INDEX IF NOT EXISTS idx_todo_categories_user_id ON todo_categories(user_id);

-- 3. Hábitos e Rotinas
CREATE TABLE IF NOT EXISTS habits (
    id UUID PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    icon VARCHAR(50) NOT NULL DEFAULT 'check-circle',
    color_hex VARCHAR(16) NOT NULL DEFAULT '#10B981',
    
    -- Agendamento e Frequência
    schedule_type VARCHAR(32) NOT NULL, -- DAILY, FIXED_WEEKLY, FLEXIBLE_WEEKLY, FIXED_MONTHLY, FLEXIBLE_MONTHLY
    days_of_week VARCHAR(64),           -- CSV ex: 'MONDAY,WEDNESDAY,FRIDAY' (para FIXED_WEEKLY)
    days_of_month VARCHAR(128),         -- CSV ex: '1,15,31' (para FIXED_MONTHLY)
    target_count_per_period INT NOT NULL DEFAULT 1, -- Cota N para FLEXIBLE_WEEKLY / FLEXIBLE_MONTHLY (ou 1 para fixos)
    quota_count_mode VARCHAR(32) NOT NULL DEFAULT 'DAYS_COMPLETED', -- DAYS_COMPLETED ou TOTAL_EXECUTIONS
    
    -- Modo de Conclusão
    completion_mode VARCHAR(32) NOT NULL DEFAULT 'CHECK', -- CHECK, QUANTITATIVE, TODO_LINKED, HEALTH_AUTO
    daily_target_value DOUBLE PRECISION NOT NULL DEFAULT 1.0, -- Ex: 2000.0 (para QUANTITATIVE ou DAILY_STEPS)
    unit VARCHAR(32),                   -- Ex: 'ml', 'páginas', 'min', 'passos', 'itens'
    
    -- Vínculo com To-Do List (Quando completion_mode = TODO_LINKED)
    linked_todo_category_id UUID REFERENCES todo_categories(id) ON DELETE SET NULL,
    
    -- Vínculo com Health Service (Quando completion_mode = HEALTH_AUTO)
    health_trigger_type VARCHAR(32),    -- WORKOUT_SESSION, DAILY_STEPS, WEIGHT_OR_BODY_LOGGED
    health_exercise_type VARCHAR(64),   -- Ex: 'ANY', 'RUNNING', 'STRENGTH_TRAINING'
    health_min_duration_minutes INT,    -- Duração mínima do treino em minutos
    
    -- Ciclo de Vida
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_habits_user_active ON habits(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_habits_linked_category ON habits(linked_todo_category_id);

-- 4. Logs de Execução de Hábitos
CREATE TABLE IF NOT EXISTS habit_execution_logs (
    id UUID PRIMARY KEY,
    habit_id UUID NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
    user_id VARCHAR(128) NOT NULL,
    execution_date DATE NOT NULL,
    completed_value DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    status VARCHAR(24) NOT NULL DEFAULT 'COMPLETED', -- COMPLETED, PARTIAL
    source VARCHAR(24) NOT NULL DEFAULT 'MANUAL',    -- MANUAL, TODO_LINKED, HEALTH_AUTO, AI_ASSISTANT
    source_reference_id VARCHAR(128),                -- ID do TodoItem ou ID do Workout/Steps/Weight
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_habit_logs_user_date ON habit_execution_logs(user_id, execution_date);
CREATE INDEX IF NOT EXISTS idx_habit_logs_habit_date ON habit_execution_logs(habit_id, execution_date);
CREATE UNIQUE INDEX IF NOT EXISTS uk_habit_log_source_ref 
    ON habit_execution_logs(habit_id, source, source_reference_id) 
    WHERE source_reference_id IS NOT NULL;

-- 5. Registro de Pulos / Congelamentos (SKIPPED)
CREATE TABLE IF NOT EXISTS habit_period_skips (
    id UUID PRIMARY KEY,
    habit_id UUID NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
    user_id VARCHAR(128) NOT NULL,
    skip_date DATE NOT NULL,            -- Data do dia pulado (ou data de início da semana/mês para skip de cota)
    skip_count INT NOT NULL DEFAULT 1,  -- Quantidade de execuções abonadas do Esperado no período
    reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_habit_skip_date UNIQUE (habit_id, skip_date)
);
CREATE INDEX IF NOT EXISTS idx_habit_skips_user_date ON habit_period_skips(user_id, skip_date);

-- 6. Itens da To-Do List (Tarefas Pontuais)
CREATE TABLE IF NOT EXISTS todo_items (
    id UUID PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category_id UUID REFERENCES todo_categories(id) ON DELETE SET NULL,
    linked_habit_id UUID REFERENCES habits(id) ON DELETE SET NULL,
    priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, URGENT
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',  -- PENDING, COMPLETED
    due_date DATE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_todo_items_user_status ON todo_items(user_id, status);
CREATE INDEX IF NOT EXISTS idx_todo_items_user_due ON todo_items(user_id, due_date);
CREATE INDEX IF NOT EXISTS idx_todo_items_linked_habit ON todo_items(linked_habit_id);

-- 7. Subtarefas (Checklist de um Item To-Do)
CREATE TABLE IF NOT EXISTS todo_subtasks (
    id UUID PRIMARY KEY,
    todo_id UUID NOT NULL REFERENCES todo_items(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_todo_subtasks_todo_id ON todo_subtasks(todo_id);
