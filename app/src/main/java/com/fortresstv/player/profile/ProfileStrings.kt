package com.fortresstv.player.profile

internal class ProfileStrings(private val portuguese: Boolean) {
    private fun text(en: String, pt: String) = if (portuguese) pt else en

    val profilesTitle = text("Profiles", "Perfis")
    val profile = text("Profile", "Perfil")
    val switchProfile = text("Switch Profile", "Mudar de Perfil")
    val manageProfiles = text("Manage Profiles", "Gerir Perfis")
    val addProfile = text("Add Profile", "Adicionar Perfil")
    val editProfile = text("Edit Profile", "Editar Perfil")
    val deleteProfile = text("Delete Profile", "Eliminar Perfil")
    val deleteConfirm = text("Are you sure you want to delete this profile?", "Tem a certeza de que pretende eliminar este perfil?")
    val profileName = text("Profile name", "Nome do perfil")
    val profileNameExists = text("A profile with this name already exists", "Já existe um perfil com este nome")
    val saving = text("Saving...", "A guardar...")
    val adult = text("Adult", "Adulto")
    val child = text("Child", "Criança")
    val adultProfile = text("Adult profile", "Perfil de adulto")
    val childProfile = text("Child profile", "Perfil de criança")
    val language = text("Language", "Idioma")
    val english = text("English", "Inglês")
    val portugueseLang = text("Portuguese (Portugal)", "Português (Portugal)")
    val ageLimit = text("Maximum age rating", "Classificação etária máxima")
    val blockUnrated = text("Block unrated content", "Bloquear conteúdo sem classificação")
    val hideAdultContent = text("Hide adult content (XXX / 18+)", "Ocultar conteúdo para adultos (XXX / 18+)")
    val parentalControls = text("Parental Controls", "Controlo Parental")
    val parentalPin = text("Parental PIN", "PIN Parental")
    val enterPin = text("Enter Adult PIN", "Introduzir PIN de Adulto")
    val setPin = text("Set Parental PIN", "Definir PIN Parental")
    val changePin = text("Change PIN", "Alterar PIN")
    val removePin = text("Remove PIN", "Remover PIN")
    val newPin = text("New PIN (4-12 digits)", "Novo PIN (4-12 dígitos)")
    val confirmPin = text("Confirm PIN", "Confirmar PIN")
    val pinMismatch = text("PINs do not match", "Os PINs não coincidem")
    val wrongPin = text("Incorrect PIN", "PIN incorreto")
    val adultRequired = text("Adult authorization required", "Autorização de adulto necessária")
    val active = text("Active", "Ativo")
    val save = text("Save", "Guardar")
    val cancel = text("Cancel", "Cancelar")
    val close = text("Close", "Fechar")
    val delete = text("Delete", "Eliminar")
    val pinProtected = text("Protected by PIN", "Protegido por PIN")
    val whoIsWatching = text("Who is watching?", "Quem está a ver?")
    val selectProfileToContinue = text("Select a profile to continue", "Selecione um perfil para continuar")
    val lastUsed = text("Last used", "Último utilizado")
    val forgotPin = text("Forgot PIN?", "Esqueceu-se do PIN?")
    val resetPin = text("Reset PIN", "Repor PIN")
    val resetHouseholdPinNotice = text("This changes the PIN for all adult profiles.", "Isto altera o PIN para todos os perfis de adulto.")
    val enterVerificationCode = text("Enter the code sent to your email", "Introduza o código enviado para o seu email")
    val verificationCode = text("Verification code / token", "Código / token de verificação")
    val sendCode = text("Send code", "Enviar código")
    val verifyCode = text("Verify code", "Verificar código")
    val invalidCode = text("Invalid or expired code", "Código inválido ou expirado")
    val newHouseholdPin = text("New PIN", "Novo PIN")
    val enterPasswordToReset = text("Or verify with account password", "Ou confirme com a palavra-passe da conta")
    val accountPassword = text("Account password", "Palavra-passe da conta")
    val wrongPassword = text("Incorrect account password", "Palavra-passe da conta incorreta")

    fun sendVerificationCode(email: String) = text(
        "Send verification code to $email?",
        "Enviar código de verificação para $email?"
    )

    fun remainingAttempts(count: Int) = text(
        "Incorrect PIN. $count attempt(s) remaining.",
        "PIN incorreto. Restam $count tentativa(s)."
    )

    fun lockedNotice(minutes: Int) = text(
        "Too many failed attempts. Locked for $minutes minute(s).",
        "Demasiadas tentativas falhadas. Bloqueado durante $minutes minuto(s)."
    )
}
