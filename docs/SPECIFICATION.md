# Registro de Decisões de Produto e Especificação Funcional — `habit_service`

> **Status**: `Refinamento Concluído e Aprovado (v1.0.0)`
> **Data**: `2026-10-28`
> **Documentação Técnica Completa**: [`PROJECT_DOCUMENTATION.md`](PROJECT_DOCUMENTATION.md)
> **Roteiro de Implementação**: [`IMPLEMENTATION_ROADMAP.md`](IMPLEMENTATION_ROADMAP.md)

---

## 1. Matriz Consolidada de Decisões (Rodadas 1, 2 e 3)

| ID | Tema | Decisão Aprovada pelo Usuário |
| :--- | :--- | :--- |
| **D-01** | **Nome da Pasta e Repositório Git** | **`habit_service`** (`c:\Repos\habit_service`), seguindo o padrão `auth_service`, `financial_service` e `health_service`. Porta HTTP dedicada: `:8083`. |
| **D-02** | **Modelo de Vínculo To-Do List ↔ Hábito/Rotina** | **Híbrido (Categoria + Vínculo Direto)**: Um hábito `TODO_LINKED` pode monitorar uma Categoria inteira de To-Do (ex: qualquer tarefa da categoria *"Casa"* concluída soma `+1` no hábito) **e/ou** uma tarefa pode indicar diretamente a qual hábito pertence (`linkedHabitId`), com deduplicação automática por `(habitId, todoId)`. |
| **D-03** | **Impacto dos Hábitos Flexíveis no Dia vs. Semana/Mês** | Na **Visão Diária**, separa-se **Fixos do Dia** (meta obrigatória do dia) de **Flexíveis da Semana/Mês** (se executados no dia, contam como `1/1` ou bônus do dia e avançam a cota do período). Na **Visão Semanal/Mensal**, soma-se tudo no comparativo "Esperado vs. Alcançado". |
| **D-04** | **Funcionalidades Avançadas Escolhidas** | Todas as 5 aprovadas: (1) Hábitos Quantitativos/Incrementais; (2) Integração `bot_gateway` / Aurora AI; (3) Streaks inteligentes para ciclos flexíveis + opção de Pular/Congelar (`SKIPPED`); (4) Auto-check integrado ao `health_service`; (5) Subtarefas (Checklist), Prioridades e Due Date na To-Do List. |
| **D-05** | **Camadas do Ecossistema Contempladas** | Especificação completa ponta a ponta: `habit_service` (Backend) + `personal-assistant-web` (Next.js 15) + `personal-assistant-mobile` (Android Compose) + `bot_gateway` (Plugin IA + Jobs) + `infra` (K8s/ArgoCD). |
| **D-06** | **Início e Fim da Semana** | **Configurável por usuário** em `user_routine_settings.week_start_day`, com **Segunda a Domingo (`MONDAY`)** como padrão inicial. |
| **D-07** | **Regra de Superação (Overachievement, ex: 4/3)** | No percentual geral de consistência da semana/mês, **trava em 100% por hábito** (`min(achieved, expected)`) para não mascarar outro hábito não cumprido; porém, no card individual do hábito e na barra absoluta, exibe o excedente (`4/3 — 133% Superação`). |
| **D-08** | **Subtarefas e Estorno ao Reabrir To-Do** | **Apenas a conclusão da tarefa principal (Pai)** soma `+1` no hábito vinculado; subtarefas servem como checklist interno. Se a tarefa principal for desmarcada (reaberta) ou excluída, **o `+1` é estornado automaticamente** do hábito. |
| **D-09** | **Gatilhos do Auto-Check com `health_service`** | Três gatilhos aprovados: (1) **Sessão de Treino realizada** (qualquer treino ou por tipo/duração mínima via `Workouts`); (2) **Meta de Passos diários atingida** (via `StepsRecord`); (3) **Registro de Peso / Bioimpedância ou Medidas Corporais** realizado (via `WeightRecord` / `BodyMeasurements`). |
| **D-10** | **Componentes Gráficos do Dashboard** | Todos os 4 aprovados: (1) **Gráfico de Barras Comparativo Duplo** (*Esperado* vs. *Alcançado* por dia da semana e semana do mês); (2) **Anéis/Gauges de Progresso** do Dia e da Semana; (3) **Heatmap de Consistência** (grade histórica estilo GitHub/HabitKit); (4) **Gráfico de Linha/Área de Evolução** da consistência ao longo das semanas. |
| **D-11** | **Organização de Navegação (Web e Mobile)** | **Seção unificada "Rotina & Hábitos"** contendo 4 abas internas (*Visão do Dia*, *To-Do List*, *Meus Hábitos* e *Gráficos/Analytics*) + Card resumo no Dashboard Inicial. |
| **D-12** | **Fronteira Hábito vs. To-Do e Atuação Proativa da IA** | Qualquer item recorrente é cadastrado como **Hábito**; a **To-Do List** é exclusiva para tarefas pontuais/projetos (que ao serem concluídas ficam no histórico com `completedAt` e acionam metas de hábitos). A **Aurora AI** inclui Hábitos/To-Dos no **Morning Briefing** e executa um **Job Noturno às 20h00 no Telegram** alertando pendências do dia ou cotas flexíveis próximas do fim do ciclo. |
