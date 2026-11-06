package com.fortresstv.player.account

internal class AccountStrings(private val portuguese: Boolean) {
    private fun text(en: String, pt: String) = if (portuguese) pt else en
    val title = text("Account", "Conta")
    val introduction = text("Sign in to your FortressTV account. Household profiles and sync will be added in a later update. Your library is currently stored on this device.", "Inicie sessão na sua conta FortressTV. Os perfis do agregado familiar e a sincronização serão adicionados numa atualização futura. A sua biblioteca está atualmente guardada neste dispositivo.")
    val email = text("Email address", "Endereço de e-mail")
    val password = text("Password", "Palavra-passe")
    val repeatPassword = text("Confirm password", "Confirmar palavra-passe")
    val signIn = text("Sign in", "Iniciar sessão")
    val register = text("Create account", "Criar conta")
    val signOut = text("Sign out", "Terminar sessão")
    val forgot = text("Forgot password?", "Esqueceu-se da palavra-passe?")
    val sendCode = text("Send recovery code", "Enviar código de recuperação")
    val resend = text("Send a new code", "Enviar um novo código")
    val code = text("Email verification code", "Código de verificação do e-mail")
    val verify = text("Verify code", "Verificar código")
    val savePassword = text("Save new password", "Guardar nova palavra-passe")
    val back = text("Back to sign in", "Voltar ao início de sessão")
    val signupSent = text("Check your email for the confirmation code. Already confirmed your email? Return to sign in.", "Consulte o seu e-mail para obter o código de confirmação. Já confirmou o e-mail? Volte ao início de sessão.")
    val recoverySent = text("If an account exists for this email, a recovery code will arrive shortly.", "Se existir uma conta com este e-mail, receberá em breve um código de recuperação.")
    val passwordSaved = text("Password changed. Sign in with your new password.", "Palavra-passe alterada. Inicie sessão com a nova palavra-passe.")
    val signedOut = text("Signed out on this device.", "Sessão terminada neste dispositivo.")
    val offlineSignOut = text("Signed out on this device. The server could not be reached to revoke the session.", "Sessão terminada neste dispositivo. Não foi possível contactar o servidor para revogar a sessão.")
    val validation = text("Enter a valid email address. New passwords must contain at least 8 characters and match the confirmation.", "Introduza um endereço de e-mail válido. As novas palavras-passe devem ter pelo menos 8 caracteres e coincidir com a confirmação.")
    val invalidCode = text("Enter the numeric code from your email.", "Introduza o código numérico recebido por e-mail.")
    val unavailable = text("Account access is not configured in this build.", "O acesso à conta não está configurado nesta versão.")
    fun signedIn(email: String) = text("Signed in as $email", "Sessão iniciada como $email")
    fun error(reason: AccountError) = when (reason) {
        AccountError.NETWORK -> text("Unable to connect. Check your connection and try again.", "Não foi possível ligar. Verifique a ligação e tente novamente.")
        AccountError.INVALID_LOGIN -> text("Check your email and password, and confirm your email before signing in.", "Verifique o e-mail e a palavra-passe e confirme o e-mail antes de iniciar sessão.")
        AccountError.EXPIRED_CODE -> text("The code is invalid or expired. Request a new code.", "O código é inválido ou expirou. Peça um novo código.")
        AccountError.RATE_LIMIT -> text("Too many attempts. Wait before trying again.", "Demasiadas tentativas. Aguarde antes de tentar novamente.")
        AccountError.WEAK_PASSWORD -> text("Choose a stronger password according to the account requirements.", "Escolha uma palavra-passe mais segura de acordo com os requisitos da conta.")
        AccountError.SESSION_EXPIRED -> text("Your session expired. Sign in again.", "A sua sessão expirou. Inicie sessão novamente.")
        AccountError.SERVICE -> text("The account service is unavailable. Try again later.", "O serviço de contas está indisponível. Tente novamente mais tarde.")
    }
}
