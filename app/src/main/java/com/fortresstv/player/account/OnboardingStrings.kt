package com.fortresstv.player.account

internal class OnboardingStrings(private val portuguese: Boolean) {
    private fun text(en: String, pt: String) = if (portuguese) pt else en

    val welcomeTitle = text("Welcome to FortressTV", "Bem-vindo ao FortressTV")
    val welcomeSubtitle = text(
        "FortressTV is a private IPTV player. Sign in to your household account to restore your profiles and source connections, or set up a new household.",
        "O FortressTV é um leitor de IPTV privado. Inicie sessão na conta do seu agregado para restaurar os perfis e fontes, ou configure um novo agregado."
    )

    val stepSignInOrRegister = text("Sign in / Create account", "Iniciar sessão / Criar conta")
    val stepPinSetup = text("Household PIN Setup", "Configurar PIN do Agregado")
    val pinSetupDescription = text(
        "Set a shared 4 to 12 digit PIN to protect adult profiles and source connections.",
        "Defina um PIN partilhado de 4 a 12 dígitos para proteger os perfis de adulto e as fontes."
    )

    val stepAddSource = text("Add Xtream Connection", "Adicionar Ligação Xtream")
    val addSourceDescription = text(
        "Connect your authorized Xtream IPTV provider. You can validate the connection now or set it up later.",
        "Ligue o seu fornecedor de IPTV Xtream autorizado. Pode validar a ligação agora ou configurá-la mais tarde."
    )

    val serverUrl = text("Server URL", "URL do Servidor")
    val username = text("Username", "Utilizador")
    val password = text("Password", "Palavra-passe")
    val sourceName = text("Connection name", "Nome da ligação")

    val validateAndSave = text("Validate & Save", "Validar e Guardar")
    val skipForNow = text("Setup later", "Configurar mais tarde")
    val validating = text("Validating connection...", "A validar ligação...")
    val connectionFailed = text("Connection failed. Check server URL and credentials.", "Falha na ligação. Verifique o URL do servidor e as credenciais.")
    val connectionSuccess = text("Connection verified successfully!", "Ligação verificada com sucesso!")

    val restoreNotice = text("Restoring household profiles and connections...", "A restaurar perfis e ligações do agregado...")
    val adultAuthRequired = text("Adult authorization required to restore household connections.", "Autorização de adulto necessária para restaurar as fontes do agregado.")
    val genericError = text("An unexpected error occurred. Please try again.", "Ocorreu um erro inesperado. Tente novamente.")
    val onboardingComplete = text("Setup Complete", "Configuração Concluída")
    val finish = text("Start Watching", "Começar a Ver")
}
