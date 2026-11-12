package com.fortresstv.player.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.fortresstv.player.account.AccountRepository
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.tvFocusHighlight
import kotlinx.coroutines.launch

enum class HouseholdPinDialogMode {
    VERIFY, SETUP
}

private enum class RecoveryStep {
    REQUEST_CODE, ENTER_CODE, SET_NEW_PIN
}

@Composable
internal fun ParentalPinDialog(
    mode: HouseholdPinDialogMode = HouseholdPinDialogMode.VERIFY,
    title: String? = null,
    onSuccess: (unlockToken: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val repository = ProfileRepository.get(androidx.compose.ui.platform.LocalContext.current)
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentEmail = remember { AccountRepository.get(context).currentEmail() }
    val languageTag = LocalAppStrings.current.languageTag
    val copy = remember(languageTag) {
        ProfileStrings(languageTag.startsWith("pt"))
    }
    val scope = rememberCoroutineScope()

    var isRecoveringPin by remember { mutableStateOf(false) }
    var recoveryStep by remember { mutableStateOf(RecoveryStep.REQUEST_CODE) }
    var recoveryOtp by remember { mutableStateOf("") }
    var showPasswordOption by remember { mutableStateOf(false) }
    var accountPasswordInput by remember { mutableStateOf("") }

    var enteredPin by remember { mutableStateOf("") }
    var setupFirstPin by remember { mutableStateOf("") }
    var isConfirmingSetup by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val status = remember { repository.getPinStatus() }
    var isLocked by remember { mutableStateOf(status.isLocked) }
    var lockoutSeconds by remember { mutableStateOf(status.lockoutSeconds) }

    LaunchedEffect(isLocked, lockoutSeconds) {
        if (isLocked && lockoutSeconds > 0) {
            errorMessage = copy.lockedNotice((lockoutSeconds + 59) / 60)
        }
    }

    fun submitPin(pin: String) {
        if (busy || isLocked) return
        if (pin.length < 4) return

        if (isRecoveringPin && recoveryStep == RecoveryStep.SET_NEW_PIN) {
            if (!isConfirmingSetup) {
                setupFirstPin = pin
                enteredPin = ""
                isConfirmingSetup = true
                errorMessage = null
            } else {
                if (pin == setupFirstPin) {
                    busy = true
                    scope.launch {
                        val recoveryToken = AccountRepository.get(context).getRecoveryAccessToken()
                        val ok = repository.resetPinAfterRecovery(pin, recoveryToken)
                        AccountRepository.get(context).discardRecovery()
                        busy = false
                        if (ok) {
                            onSuccess(null)
                        } else {
                            errorMessage = copy.wrongPin
                            isConfirmingSetup = false
                            enteredPin = ""
                            setupFirstPin = ""
                        }
                    }
                } else {
                    errorMessage = copy.pinMismatch
                    enteredPin = ""
                    setupFirstPin = ""
                    isConfirmingSetup = false
                }
            }
            return
        }

        if (mode == HouseholdPinDialogMode.SETUP) {
            if (!isConfirmingSetup) {
                setupFirstPin = pin
                enteredPin = ""
                isConfirmingSetup = true
                errorMessage = null
            } else {
                if (pin == setupFirstPin) {
                    busy = true
                    scope.launch {
                        val ok = repository.setPin(pin)
                        busy = false
                        if (ok) {
                            onSuccess(null)
                        } else {
                            errorMessage = copy.wrongPin
                            isConfirmingSetup = false
                            enteredPin = ""
                            setupFirstPin = ""
                        }
                    }
                } else {
                    errorMessage = copy.pinMismatch
                    enteredPin = ""
                    setupFirstPin = ""
                    isConfirmingSetup = false
                }
            }
            return
        }

        // Mode VERIFY
        busy = true
        errorMessage = null
        scope.launch {
            val result = repository.verifyPin(pin)
            busy = false
            when (result) {
                is PinVerifyResult.Success -> {
                    onSuccess(result.unlockToken)
                }
                is PinVerifyResult.Locked -> {
                    isLocked = true
                    lockoutSeconds = result.lockoutSeconds
                    errorMessage = copy.lockedNotice((result.lockoutSeconds + 59) / 60)
                    enteredPin = ""
                }
                is PinVerifyResult.Failed -> {
                    errorMessage = copy.remainingAttempts(result.remainingAttempts)
                    enteredPin = ""
                }
                PinVerifyResult.Unavailable -> {
                    errorMessage = copy.pinVerificationUnavailable
                    enteredPin = ""
                }
            }
        }
    }

    fun onDigit(d: String) {
        if (enteredPin.length < 12 && !busy && !isLocked) {
            enteredPin += d
        }
    }

    fun onBackspace() {
        if (enteredPin.isNotEmpty() && !busy && !isLocked) {
            enteredPin = enteredPin.dropLast(1)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .width(380.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isRecoveringPin && recoveryStep != RecoveryStep.SET_NEW_PIN) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = copy.resetPin,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = copy.resetHouseholdPinNotice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (recoveryStep == RecoveryStep.REQUEST_CODE) {
                        if (showPasswordOption) {
                            Text(
                                text = copy.enterPasswordToReset,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = accountPasswordInput,
                                onValueChange = { accountPasswordInput = it },
                                label = { Text(copy.accountPassword) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (errorMessage != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = { isRecoveringPin = false; showPasswordOption = false; errorMessage = null },
                                    modifier = Modifier.tvFocusHighlight()
                                ) {
                                    Text(copy.cancel)
                                }
                                Button(
                                    enabled = accountPasswordInput.isNotBlank() && !busy,
                                    onClick = {
                                        busy = true
                                        errorMessage = null
                                        scope.launch {
                                            val res = runCatching {
                                                AccountRepository.get(context).signIn(currentEmail.orEmpty(), accountPasswordInput)
                                            }
                                            busy = false
                                            if (res.isSuccess) {
                                                recoveryStep = RecoveryStep.SET_NEW_PIN
                                                setupFirstPin = ""
                                                enteredPin = ""
                                                isConfirmingSetup = false
                                            } else {
                                                errorMessage = copy.wrongPassword
                                            }
                                        }
                                    },
                                    modifier = Modifier.tvFocusHighlight()
                                ) {
                                    Text(copy.verifyCode)
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            TextButton(
                                onClick = { showPasswordOption = false; errorMessage = null },
                                modifier = Modifier.tvFocusHighlight()
                            ) {
                                Text(copy.sendCode)
                            }
                        } else {
                            Text(
                                text = copy.sendVerificationCode(currentEmail.orEmpty()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (errorMessage != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = { isRecoveringPin = false; errorMessage = null },
                                    modifier = Modifier.tvFocusHighlight()
                                ) {
                                    Text(copy.cancel)
                                }
                                Button(
                                    enabled = !busy && !currentEmail.isNullOrBlank(),
                                    onClick = {
                                        busy = true
                                        errorMessage = null
                                        scope.launch {
                                            val ok = runCatching {
                                                AccountRepository.get(context).requestRecovery(currentEmail.orEmpty())
                                            }.isSuccess
                                            busy = false
                                            if (ok) {
                                                recoveryStep = RecoveryStep.ENTER_CODE
                                                recoveryOtp = ""
                                            } else {
                                                errorMessage = copy.invalidCode
                                            }
                                        }
                                    },
                                    modifier = Modifier.tvFocusHighlight()
                                ) {
                                    Text(copy.sendCode)
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            TextButton(
                                onClick = { showPasswordOption = true; errorMessage = null },
                                modifier = Modifier.tvFocusHighlight()
                            ) {
                                Text(copy.enterPasswordToReset)
                            }
                        }
                    } else if (recoveryStep == RecoveryStep.ENTER_CODE) {
                        Text(
                            text = copy.enterVerificationCode,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = recoveryOtp,
                            onValueChange = { recoveryOtp = it.trim() },
                            label = { Text(copy.verificationCode) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = { isRecoveringPin = false; errorMessage = null },
                                modifier = Modifier.tvFocusHighlight()
                            ) {
                                Text(copy.cancel)
                            }
                            Button(
                                enabled = recoveryOtp.isNotBlank() && !busy,
                                onClick = {
                                    busy = true
                                    errorMessage = null
                                    scope.launch {
                                        val ok = runCatching {
                                            AccountRepository.get(context).verifyRecovery(currentEmail.orEmpty(), recoveryOtp)
                                        }.isSuccess
                                        busy = false
                                        if (ok) {
                                            recoveryStep = RecoveryStep.SET_NEW_PIN
                                            setupFirstPin = ""
                                            enteredPin = ""
                                            isConfirmingSetup = false
                                        } else {
                                            errorMessage = copy.invalidCode
                                        }
                                    }
                                },
                                modifier = Modifier.tvFocusHighlight()
                            ) {
                                Text(copy.verifyCode)
                            }
                        }
                    }
                } else {
                    // Keypad Mode: verify, setup, or recovery step SET_NEW_PIN
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = if (isRecoveringPin) {
                            if (isConfirmingSetup) copy.confirmPin else copy.newHouseholdPin
                        } else title ?: when {
                            mode == HouseholdPinDialogMode.SETUP && isConfirmingSetup -> copy.confirmPin
                            mode == HouseholdPinDialogMode.SETUP -> copy.setPin
                            else -> copy.enterPin
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (isRecoveringPin) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = copy.resetHouseholdPinNotice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // PIN dots indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val count = enteredPin.length.coerceAtLeast(4)
                        for (i in 0 until count) {
                            val filled = i < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (filled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Keypad 3x4
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("back", "0", "ok")
                    )

                    for (row in keys) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (key in row) {
                                when (key) {
                                    "back" -> {
                                        OutlinedButton(
                                            onClick = ::onBackspace,
                                            enabled = enteredPin.isNotEmpty() && !busy && !isLocked,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .size(64.dp)
                                                .padding(4.dp)
                                                .tvFocusHighlight()
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "Backspace",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    "ok" -> {
                                        Button(
                                            onClick = { submitPin(enteredPin) },
                                            enabled = enteredPin.length >= 4 && !busy && !isLocked,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier
                                                .size(64.dp)
                                                .padding(4.dp)
                                                .tvFocusHighlight()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Confirm",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        Button(
                                            onClick = { onDigit(key) },
                                            enabled = !busy && !isLocked,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier
                                                .size(64.dp)
                                                .padding(4.dp)
                                                .tvFocusHighlight()
                                        ) {
                                            Text(text = key, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    if (!isRecoveringPin && mode == HouseholdPinDialogMode.VERIFY && currentEmail != null) {
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                isRecoveringPin = true
                                recoveryStep = RecoveryStep.REQUEST_CODE
                                errorMessage = null
                            },
                            modifier = Modifier.tvFocusHighlight()
                        ) {
                            Text(copy.forgotPin)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusHighlight()
                    ) {
                        Text(copy.cancel)
                    }
                }
            }
        }
    }
}
