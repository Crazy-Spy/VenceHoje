# 🚀 VenceHoje - Lembrete de Contas

O **VenceHoje** é um assistente pessoal leve e totalmente offline para Android, focado em ajudar você a nunca mais esquecer um prazo de vencimento. O objetivo é simples: cadastrar, alertar e evitar que você jogue dinheiro fora com juros e multas.

---

## ✨ Funcionalidades

- **Cadastro Rápido:** Adicione contas, valores e categorias em poucos segundos.
- **Alertas Inteligentes:** Notificações configuráveis para te avisar antes e no dia do vencimento.
- **Dashboard Visual:** Gráficos intuitivos para ver o que já foi pago e o que ainda está pendente no mês.
- **Controle de Encargos:** Diferenciação visual entre o valor original da conta e os juros pagos (fatia de "Encargos" no gráfico).
- **Importação/Exportação CSV:** Controle total dos seus dados com backups manuais fáceis de gerar.
- **100% Offline:** Privacidade total. Seus dados financeiros não saem do seu celular por nada.

## 🛠️ Tecnologias Utilizadas

- **Linguagem:** [Kotlin](https://kotlinlang.org/)
- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Interface moderna e declarativa)
- **Banco de Dados:** [Room SQLite](https://developer.android.com/training/data-storage/room) (Persistência local)
- **Background Tasks:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) (Para agendar as notificações)
- **Arquitetura:** MVVM (Model-View-ViewModel)

## 🔒 Privacidade e Segurança

O **VenceHoje** não solicita permissões de internet, não utiliza SDKs de terceiros para rastreamento e não exige criação de conta ou login. Toda a lógica de armazenamento é feita através do SQLite interno do Android.
