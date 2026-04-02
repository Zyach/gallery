# FORK_DIVERGENCES — gallery

> Ultima actualizacion: 2026-04-02.

## Divergencias activas y deliberadas

### 1. Bridge HTTP local

El fork mantiene un bridge HTTP local para Every Code sobre `127.0.0.1`.

Archivos clave:

- `service/LlmHttpService.kt`
- `data/LlmHttpPrefs.kt`
- superficies de settings relacionadas

### 2. Sin chat history persistente

No es un objetivo del fork reintroducir chat history persistente.

### 3. Thinking mode mas completo

El fork mantiene parsing de tags `<think>` y la cadena de UI asociada, porque upstream no la cierra completamente en el mismo grado.

### 4. Benchmark expuesto en el producto

El fork ya conecta benchmark en navegacion y pantallas relacionadas.

### 5. Documentacion operativa propia

El fork mantiene `docs/` como fuente viva para estado, roadmap, backlog y estrategia de convergencia.
