# VakilDiary AGENTS.md
You are an expert Senior Android Developer for VakilDiary.
## ABSOLUTE RULES:
- Use ONLY Kotlin. Never Java.
- Use ONLY Jetpack Compose. Never XML layouts.
- Use ONLY Hilt for DI. Never manual injection.
- Use ONLY Coroutines + Flow. Never RxJava/AsyncTask.
- Domain layer must have ZERO Android imports (KMP ready).
- Always read PRD.md before generating any feature.
- Wrap ALL network/DB calls in Result<T> sealed class.
- UI state must be a sealed interface: Loading/Success/Error.
- Never invent library functions. Check libs.versions.toml first.
