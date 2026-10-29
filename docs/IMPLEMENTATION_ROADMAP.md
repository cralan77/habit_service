# Plano de Implementação Passo a Passo para Agentes de IA — `habit_service` & Ecossistema

> **Versão**: `1.0.0`
> **Data**: `2026-10-28`
> **Referência Principal**: [`PROJECT_DOCUMENTATION.md`](PROJECT_DOCUMENTATION.md)
> **Objetivo**: Roteiro determinístico dividido em 6 Fases para que um Agente Implementador codifique, teste e integre o módulo de Hábitos, Rotinas e To-Do List sem ambiguidades.

---

## Fase 1 — Criação do Microsserviço Backend (`c:\Repos\habit_service`)

### 1.1 Inicialização do Repositório e Build
- [ ] Inicializar repositório Git em `c:\Repos\habit_service` (`git init`, branch `main` e branch `homolog`).
- [ ] Criar `settings.gradle.kts` (`rootProject.name = "personal-habit-management"`), `build.gradle.kts` (Kotlin `2.2.20`, Spring Boot `4.0.7`, Java 21, Spring Data JPA, Flyway, PostgreSQL, Spring Security, JJWT `0.12.6`, Actuator, Micrometer Prometheus, SpringDoc OpenAPI `2.8.5`, Kotest, Mockito-Kotlin e H2 para testes).
- [ ] Copiar o Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`) a partir de `c:\Repos\health_service` para garantir paridade exata de build local e CI/CD.
- [ ] Criar `Dockerfile` (multi-stage build com `eclipse-temurin:21-jdk-alpine` e `eclipse-temurin:21-jre-alpine`, expondo porta `8083`) e `docker-compose.yml` (PostgreSQL local na porta `5435:5432`, DB `habit`).
- [ ] Criar `.github/workflows/ci-cd.yml` seguindo o padrão de `health_service` (build + testes + publicação de imagem no GHCR `ghcr.io/cralan77/habit-service`).

### 1.2 Banco de Dados e Persistência
- [ ] Criar `src/main/resources/application.yml` na porta `8083` com datasource PostgreSQL, Flyway habilitado e chave `jwt.secret`.
- [ ] Criar migration `src/main/resources/db/migration/V1__init_habit_tables.sql` exatamente conforme a **Seção 5** do `PROJECT_DOCUMENTATION.md` (7 tabelas: `user_routine_settings`, `todo_categories`, `habits`, `habit_execution_logs`, `habit_period_skips`, `todo_items`, `todo_subtasks`).
- [ ] Implementar `Entities.kt`, `SpringDataRepositories.kt`, `RepositoryImplementations.kt` e `UserIdentifierResolver.kt` em `com.habit.infrastructure.persistence`.

### 1.3 Camada de Domínio e Motores Matemáticos
- [ ] Criar modelos e enums puros em `com.habit.domain.model`:
  - `ScheduleType`: `DAILY`, `FIXED_WEEKLY`, `FLEXIBLE_WEEKLY`, `FIXED_MONTHLY`, `FLEXIBLE_MONTHLY`
  - `QuotaCountMode`: `DAYS_COMPLETED`, `TOTAL_EXECUTIONS`
  - `CompletionMode`: `CHECK`, `QUANTITATIVE`, `TODO_LINKED`, `HEALTH_AUTO`
  - `HealthTriggerType`: `WORKOUT_SESSION`, `DAILY_STEPS`, `WEIGHT_OR_BODY_LOGGED`
  - `HabitLogStatus`: `COMPLETED`, `PARTIAL`
  - `HabitLogSource`: `MANUAL`, `TODO_LINKED`, `HEALTH_AUTO`, `AI_ASSISTANT`
  - `TodoPriority`: `LOW`, `MEDIUM`, `HIGH`, `URGENT`
  - `TodoStatus`: `PENDING`, `COMPLETED`
- [ ] Implementar os 4 serviços de domínio em `com.habit.domain.service`:
  1. `HabitScheduleEvaluatorService.kt`: Cálculo de janelas semanais respeitando `weekStartDay` configurável pelo usuário (padrão `MONDAY`), validação de dias fixos da semana/mês (incluindo ajuste de dia 31 para meses de 28/29/30 dias).
  2. `HabitAnalyticsEngineService.kt`:
     - Visão Diária separando **Fixos do Dia** (meta principal) e **Flexíveis do Período** (com bônus diário quando executados hoje).
     - Visão Semanal/Mensal consolidando Fixos + Cotas Flexíveis, aplicando **cap de 100% por hábito** na taxa percentual geral (`achievedCapped`) e preservando o valor bruto (`achievedRaw` e badge `OVERACHIEVED`) quando houver superação (ex: `4/3`).
     - Abono de denominador quando houver registros em `habit_period_skips` (`SKIPPED`).
  3. `HabitStreakCalculatorService.kt`:
     - Contagem em dias agendados consecutivos para hábitos fixos (ignorando dias não agendados e dias `SKIPPED`).
     - Contagem em semanas/meses consecutivos de cota batida para hábitos flexíveis (`FLEXIBLE_WEEKLY` e `FLEXIBLE_MONTHLY`).
  4. `TodoHabitSyncDomainService.kt`:
     - Resolução sem duplicidade (`Set<UUID>`) de hábitos impactados pela conclusão de uma tarefa principal (`linkedHabitId` direto $\cup$ hábitos `TODO_LINKED` que monitoram `todo.categoryId`).
     - Geração automática de `HabitExecutionLog` ao concluir a tarefa principal (`TodoItem`) e **estorno imediato (remoção do log)** se a tarefa for reaberta (`PENDING`) ou excluída.
     - Garantia de que subtarefas (`TodoSubtask`) não disparam contadores de hábito.

### 1.4 Casos de Uso, Segurança e Controllers REST
- [ ] Implementar casos de uso em `com.habit.application.*` e registrá-los em `BeanConfig.kt`.
- [ ] Implementar `SecurityConfig`, `JwtAuthenticationFilter` e `JwtTokenValidator` em `com.habit.interfaces.rest.security`.
- [ ] Implementar `HabitController`, `TodoController`, `RoutineAnalyticsController`, `RoutineSettingsController` e `GlobalExceptionHandler` em `com.habit.interfaces.rest`.

### 1.5 Suíte de Testes Unitários Obrigatórios (`src/test/kotlin/com/habit/`)
- [ ] `HabitScheduleEvaluatorServiceTest.kt`: Testa janelas de semana começando na Segunda (`MONDAY`) e no Domingo (`SUNDAY`), e meses de 28/30/31 dias.
- [ ] `HabitAnalyticsEngineServiceTest.kt`:
  - Testa separação de Fixos vs. Flexíveis no dia.
  - Testa superação (`4/3` em Hábito A e `2/3` em Hábito B $\rightarrow$ taxa geral = `5/6 = 83.33%`, e Hábito A com `percentageRaw = 133.3%` e `OVERACHIEVED`).
  - Testa redução do esperado ao registrar `SKIPPED`.
- [ ] `HabitStreakCalculatorServiceTest.kt`: Testa streaks de hábitos diários, semanais fixos (`MON, WED, FRI`), semanais flexíveis (`3x/semana`) e preservação de streak com `SKIPPED`.
- [ ] `TodoHabitSyncDomainServiceTest.kt`:
  - Testa hábito *"Executar 2 itens de To-Do List por mês"*: concluir 2 tarefas da categoria vinculada marca o hábito mensal como `2/2 (100% Sucesso)`.
  - Testa deduplicação quando a tarefa tem `categoryId` e `linkedHabitId` apontando para o mesmo hábito.
  - Testa que marcar subtarefa não incrementa hábito.
  - Testa **estorno automático** (`2/2 -> 1/2`) ao desmarcar uma tarefa concluída.

---

## Fase 2 — Integração de Auto-Check com `health_service` (`c:\Repos\health_service`)

- [ ] Adicionar propriedade `habit.service.url` em `c:\Repos\health_service\src\main\resources\application.yml`.
- [ ] Criar adaptador `HabitEventClient.kt` em `com.health.infrastructure` que envia `POST /api/habits/internal/health-events` de forma resiliente (timeout curto + `runCatching`) ao concluir:
  - `SyncWorkoutUseCase` (`eventType = WORKOUT_SESSION`)
  - `SyncActivityUseCase.recordSteps` (`eventType = DAILY_STEPS`)
  - `RecordWeightUseCase` e `RecordBodyMeasurementUseCase` (`eventType = WEIGHT_OR_BODY_LOGGED`)
- [ ] Implementar no `habit_service` o endpoint `POST /api/habits/sync-health` para reconciliação ativa consultando `GET /api/health/summary`.

---

## Fase 3 — Integração com `bot_gateway` / Aurora AI (`c:\Repos\bot_gateway`)

- [ ] Adicionar `HabitProperties` (`habit.backend.url`, padrão `http://localhost:8083`) em `config/AppProperties.kt` e `RestClient` dedicado em `config/RestClientConfig.kt`.
- [ ] Criar `HabitTodoPluginClient.kt` em `infrastructure/plugin/` implementando `AgentPlugin` com as **8 ferramentas Gemini**:
  - `get_daily_routine_status`, `check_habit`, `skip_habit`, `create_habit`, `list_todos`, `create_todo`, `complete_todo`, `get_habit_analytics`.
- [ ] Registrar `HabitTodoPluginClient` no `PluginRegistry.kt` (domínio `HABITS` e `ALL`).
- [ ] Atualizar `AssistantOrchestratorService.kt` para incluir o resumo de hábitos e tarefas do dia no **Morning Briefing**.
- [ ] Criar `EveningHabitReminderJobService.kt` em `application/service/` com `@Scheduled(cron = "0 0 20 * * *")` para enviar alerta proativo às 20h00 no Telegram caso existam hábitos fixos do dia pendentes, cotas flexíveis próximas do fim da semana/mês ou To-Dos vencendo hoje.
- [ ] Adicionar testes unitários para `HabitTodoPluginClient` e `EveningHabitReminderJobService` mantendo a cobertura JaCoCo $\ge 75\%$.

---

## Fase 4 — Implementação no Frontend Web (`c:\Repos\personal-assistant-web`)

- [ ] **BFF Proxy**: Atualizar `src/app/api/proxy/[...path]/route.ts` adicionando roteamento de `habits`, `todos`, `todo-categories` e `routines` para `HABIT_SERVICE_URL` (`http://localhost:8083`).
- [ ] **Tipos, API e Hooks**:
  - Adicionar interfaces TypeScript em `src/types/index.ts`.
  - Criar `src/lib/api/routines.ts` e `src/lib/hooks/use-routines.ts` (TanStack Query v5 com invalidação cruzada: ao dar toggle em um To-Do, invalidar `['todos']`, `['routines', 'daily']` e `['routines', 'analytics']`).
- [ ] **Componentes e Página Unificada `/routines`**:
  - Atualizar `src/components/layout/sidebar.tsx` adicionando o item **"Rotina & Hábitos"** (`/routines`).
  - Criar `src/app/(dashboard)/routines/page.tsx` com as 4 abas internas:
    1. **Visão do Dia (`Hoje`)**: Anéis de Progresso (`RoutineDailyGauges`), Lista de Hábitos Fixos do Dia (com check, incremento quantitativo e skip), Lista de Hábitos Flexíveis da Semana/Mês (com indicadores de cota `2/3`) e Tarefas de Hoje/Urgentes.
    2. **To-Do List**: Filtro por categorias, prioridades e status, checklist expansível de subtarefas, badge de hábito vinculado e modal de criação/edição (`TodoModal`).
    3. **Meus Hábitos**: Catálogo de hábitos por frequência, streaks (`🔥`), modal de criação/edição (`HabitModal`) e seletor de dia de início da semana (`SettingsModal`).
    4. **Gráficos & Analytics**:
       - Gráfico de Barras Comparativo Duplo *Esperado vs. Alcançado* (`ExpectedVsAchievedChart.tsx` em Recharts);
       - Breakdown Horizontal por Hábito com destaque para Superação (`4/3`);
       - Heatmap de Consistência (`HabitHeatmap.tsx`);
       - Gráfico de Linha/Área de Evolução Semanal (`ConsistencyTrendChart.tsx`).
  - Adicionar Card de Resumo de Rotina na Home (`src/app/(dashboard)/page.tsx`) e opção de contexto `"HABITS"` no Chat da Aurora (`src/app/(dashboard)/assistant/page.tsx`).

---

## Fase 5 — Implementação no App Android (`c:\Repos\personal-assistant-mobile`)

- [ ] **Configuração de Rede e DI**:
  - Adicionar `HABIT_BACKEND_URL` (`http://10.0.2.2:8083/`) em `app/build.gradle.kts` e `NetworkClient.kt`.
  - Adicionar `HabitApiService` em `core/network/ApiServices.kt` e DTOs em `core/network/HabitDtos.kt`.
- [ ] **Domain & Data**:
  - Criar `domain/model/HabitModels.kt`, `domain/repository/HabitRepository.kt`, `data/repository/HabitRepositoryImpl.kt` e UseCases em `domain/usecase/habit/`.
  - Registrar dependências no Koin (`di/AppModule.kt`).
- [ ] **UI Jetpack Compose**:
  - Adicionar aba **"Rotina"** em `presentation/navigation/NavigationItem.kt` e `AppNavigation.kt`.
  - Criar `RoutineViewModel.kt` e `RoutineScreen.kt` com as 4 abas (*Hoje*, *To-Do*, *Hábitos*, *Gráficos*), componentes gráficos em `Canvas` (`ExpectedVsAchievedCanvasChart.kt`, `RoutineRingsCanvas.kt`, `HabitHeatmapCanvas.kt`) e BottomSheets de criação de Hábito e To-Do.
  - Adicionar Card rápido de Hábitos & To-Dos na `HomeScreen.kt`.

---

## Fase 6 — Manifestos GitOps Kubernetes & ArgoCD (`c:\Repos\infra`)

- [ ] Criar `apps/habit-service.yaml` e `apps-homolog/habit-service-homolog.yaml` (Deployment + Service na porta `8083`, variáveis de banco `appdb` / `appdb_homolog`, `JWT_SECRET`, `HEALTH_SERVICE_URL`).
- [ ] Criar `argocd/argocd-app-habit-service.yaml` e `argocd/argocd-app-habit-service-homolog.yaml`.
- [ ] Atualizar `personal-assistant-web.yaml`, `bot-gateway.yaml` e `health-service.yaml` (em `apps/` e `apps-homolog/`) adicionando a variável de ambiente `HABIT_SERVICE_URL`.
