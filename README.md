# `habit_service` — Microsserviço de Hábitos, Rotinas e To-Do List

> **Status**: `Especificação Completa (Pronto para Implementação)`
> **Stack**: Kotlin 2.2.20 / Spring Boot 4.0.7 / Java 21 / DDD / PostgreSQL / Flyway
> **Porta Padrão**: `:8083`

## 📚 Documentação para Desenvolvedores e Agentes de IA

- **[`docs/PROJECT_DOCUMENTATION.md`](docs/PROJECT_DOCUMENTATION.md)**: Especificação técnica e arquitetural completa (Regras de negócio, Fórmulas matemáticas de *Esperado vs. Alcançado*, Motor de Streaks Fixos/Flexíveis, Vínculo Híbrido To-Do $\leftrightarrow$ Hábito com estorno, Auto-Check com `health_service`, DDL SQL Flyway, Contratos JSON REST e integrações com `personal-assistant-web`, `personal-assistant-mobile`, `bot_gateway` e `infra`).
- **[`docs/IMPLEMENTATION_ROADMAP.md`](docs/IMPLEMENTATION_ROADMAP.md)**: Roteiro de implementação passo a passo dividido em 6 fases (Backend, Health Auto-Check, Bot Gateway/Aurora AI, Web Next.js 15, Mobile Android Compose e Kubernetes/ArgoCD).
- **[`docs/SPECIFICATION.md`](docs/SPECIFICATION.md)**: Matriz consolidada das 12 decisões de produto (`D-01` a `D-12`).
- **[`llms.txt`](llms.txt)**: Resumo estruturado de leitura rápida para LLMs e Agentes de IA.

---

## 🚀 Como Inicializar o Repositório Git

Quando iniciar a codificação deste módulo, execute na pasta `c:\Repos\habit_service`:

```powershell
cd c:\Repos\habit_service
git init
git checkout -b main
git add .
git commit -m "docs: initial specification and architecture for habit_service"
```
