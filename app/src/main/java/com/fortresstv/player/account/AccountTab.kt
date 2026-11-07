package com.fortresstv.player.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.tvFocusHighlight
import com.fortresstv.player.profile.ProfileRepository
import com.fortresstv.player.profile.ParentalPinDialog
import com.fortresstv.player.profile.HouseholdPinDialogMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class AccountPage { LOGIN, REGISTER, SIGNUP_CODE, RECOVER, RECOVERY_CODE, NEW_PASSWORD }

@Composable
internal fun AccountTab(
    modifier: Modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),
    showHeader: Boolean = true,
    onSignedOut: () -> Unit,
    onSignedIn: (() -> Unit)? = null
) {
    val context = LocalContext.current.applicationContext
    val repository = remember { AccountRepository.get(context) }
    val copy = AccountStrings(LocalAppStrings.current.languageTag.startsWith("pt"))
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(AccountPage.LOGIN) }
    var signedInEmail by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    // Keep passwords/codes out of saved instance state and preferences.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<AccountError?>(null) }
    var validationError by remember { mutableStateOf(false) }
    var codeError by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    val profileRepository = remember { ProfileRepository.get(context) }
    var showSignOutPinDialog by remember { mutableStateOf(false) }

    fun navigate(next: AccountPage) {
        repository.discardRecovery()
        page = next
        password = ""
        confirmation = ""
        code = ""
        error = null
        validationError = false
        codeError = false
        notice = null
    }

    fun doSignOut() {
        onSignedOut()
        busy = true
        error = null
        scope.launch {
            try {
                profileRepository.prepareAccountDeparture()
                val revoked = repository.signOut()
                profileRepository.onAccountChanged(null)
                signedInEmail = null
                navigate(AccountPage.LOGIN)
                notice = if (revoked) copy.signedOut else copy.offlineSignOut
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = AccountError.SERVICE }
            finally { busy = false }
        }
    }

    if (showSignOutPinDialog) {
        ParentalPinDialog(
            mode = HouseholdPinDialogMode.VERIFY,
            onSuccess = {
                showSignOutPinDialog = false
                doSignOut()
            },
            onDismiss = { showSignOutPinDialog = false }
        )
    }

    LaunchedEffect(repository) {
        try {
            repository.discardRecovery()
            signedInEmail = repository.restore()
            profileRepository.onAccountChanged(repository.currentUserId())
            profileRepository.refresh()
        }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = AccountError.SERVICE }
        finally { busy = false }
    }

    fun submit() {
        val isNewPassword = page == AccountPage.REGISTER || page == AccountPage.NEW_PASSWORD
        val needsEmail = page != AccountPage.NEW_PASSWORD
        validationError = (needsEmail && !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) ||
            (isNewPassword && (password.length < 8 || password != confirmation)) ||
            (page == AccountPage.LOGIN && password.isEmpty())
        codeError = (page == AccountPage.SIGNUP_CODE || page == AccountPage.RECOVERY_CODE) &&
            !code.trim().matches(Regex("[0-9]{6,10}"))
        if (validationError || codeError) return
        error = null
        notice = null
        busy = true
        val submittedPassword = password
        // Clear sensitive fields as soon as their request starts.
        password = ""
        confirmation = ""
        scope.launch {
            try {
                when (page) {
                    AccountPage.LOGIN -> {
                        profileRepository.prepareAccountDeparture()
                        signedInEmail = repository.signIn(email, submittedPassword)
                        profileRepository.onAccountChanged(repository.currentUserId())
                        profileRepository.refresh()
                        onSignedIn?.invoke()
                    }
                    AccountPage.REGISTER -> {
                        profileRepository.prepareAccountDeparture()
                        if (repository.register(email, submittedPassword)) {
                            signedInEmail = email.trim()
                            profileRepository.onAccountChanged(repository.currentUserId())
                            profileRepository.refresh()
                            onSignedIn?.invoke()
                        } else {
                            page = AccountPage.SIGNUP_CODE
                            notice = copy.signupSent
                        }
                    }
                    AccountPage.SIGNUP_CODE -> {
                        profileRepository.prepareAccountDeparture()
                        signedInEmail = repository.verifySignup(email, code)
                        code = ""
                        profileRepository.onAccountChanged(repository.currentUserId())
                        profileRepository.refresh()
                        onSignedIn?.invoke()
                    }
                    AccountPage.RECOVER -> {
                        repository.requestRecovery(email)
                        page = AccountPage.RECOVERY_CODE
                        notice = copy.recoverySent
                    }
                    AccountPage.RECOVERY_CODE -> { repository.verifyRecovery(email, code); code = ""; page = AccountPage.NEW_PASSWORD }
                    AccountPage.NEW_PASSWORD -> {
                        repository.resetPassword(submittedPassword)
                        page = AccountPage.LOGIN
                        notice = copy.passwordSaved
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: AccountException) { error = failure.reason }
            catch (_: Exception) { error = AccountError.SERVICE }
            finally { busy = false }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showHeader) {
            Text(copy.title, style = MaterialTheme.typography.titleLarge)
            Text(copy.introduction, style = MaterialTheme.typography.bodyMedium)
        }
        if (!AccountConfiguration.configured) { Text(copy.unavailable); return@Column }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        notice?.let { Text(it) }
        error?.let { Text(copy.error(it), color = MaterialTheme.colorScheme.error) }
        if (validationError) Text(copy.validation, color = MaterialTheme.colorScheme.error)
        if (codeError) Text(copy.invalidCode, color = MaterialTheme.colorScheme.error)

        if (signedInEmail != null) {
            Text(copy.signedIn(signedInEmail!!))
            Button(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).tvFocusHighlight(),
                onClick = {
                    val status = profileRepository.getPinStatus()
                    if (profileRepository.activeProfile.value.isChild && status.hasPin && !profileRepository.isAdultUnlocked()) {
                        showSignOutPinDialog = true
                    } else {
                        doSignOut()
                    }
                }) { Text(copy.signOut) }
        } else {
            if (page != AccountPage.NEW_PASSWORD) OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text(copy.email) }, singleLine = true,
                enabled = !busy && page != AccountPage.SIGNUP_CODE && page != AccountPage.RECOVERY_CODE,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
            if (page == AccountPage.LOGIN || page == AccountPage.REGISTER || page == AccountPage.NEW_PASSWORD) {
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(copy.password) },
                    enabled = !busy, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
            }
            if (page == AccountPage.REGISTER || page == AccountPage.NEW_PASSWORD) {
                OutlinedTextField(value = confirmation, onValueChange = { confirmation = it }, label = { Text(copy.repeatPassword) },
                    enabled = !busy, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
            }
            if (page == AccountPage.SIGNUP_CODE || page == AccountPage.RECOVERY_CODE) {
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text(copy.code) },
                    enabled = !busy, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth())
            }
            Button(onClick = ::submit, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).tvFocusHighlight()) {
                Text(when (page) {
                    AccountPage.LOGIN -> copy.signIn
                    AccountPage.REGISTER -> copy.register
                    AccountPage.RECOVER -> copy.sendCode
                    AccountPage.SIGNUP_CODE, AccountPage.RECOVERY_CODE -> copy.verify
                    AccountPage.NEW_PASSWORD -> copy.savePassword
                })
            }
            if (page == AccountPage.LOGIN) {
                TextButton(onClick = { navigate(AccountPage.REGISTER) }, enabled = !busy) { Text(copy.register) }
                TextButton(onClick = { navigate(AccountPage.RECOVER) }, enabled = !busy) { Text(copy.forgot) }
            } else TextButton(onClick = { navigate(AccountPage.LOGIN) }, enabled = !busy) { Text(copy.back) }
            if (page == AccountPage.SIGNUP_CODE || page == AccountPage.RECOVERY_CODE) {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    error = null
                    code = ""
                    scope.launch {
                        try {
                            if (page == AccountPage.SIGNUP_CODE) repository.resendSignup(email)
                            else repository.requestRecovery(email)
                            notice = if (page == AccountPage.SIGNUP_CODE) copy.signupSent else copy.recoverySent
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (failure: AccountException) { error = failure.reason }
                        catch (_: Exception) { error = AccountError.SERVICE }
                        finally { busy = false }
                    }
                }) { Text(copy.resend) }
            }
        }
    }
}
