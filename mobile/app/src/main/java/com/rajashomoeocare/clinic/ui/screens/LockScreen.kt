package com.rajashomoeocare.clinic.ui.screens

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rajashomoeocare.clinic.R
import com.rajashomoeocare.clinic.ui.components.BrandLockup
import com.rajashomoeocare.clinic.ui.vm.LockError
import com.rajashomoeocare.clinic.ui.vm.LockStage
import com.rajashomoeocare.clinic.ui.vm.LockViewModel

@Composable
fun LockScreen(
    viewModel: LockViewModel,
    activity: FragmentActivity,
    onUnlocked: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.stage) {
        if (state.stage == LockStage.UNLOCKED) onUnlocked()
    }

    LaunchedEffect(state.biometricEnabled, state.stage) {
        if (state.stage == LockStage.ENTER && state.biometricEnabled && canUseBiometrics(context)) {
            promptBiometric(activity, context, viewModel::unlockViaBiometric)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.8f))
            BrandLockup(markSize = 72)

            Spacer(Modifier.height(48.dp))

            Text(
                text = stringResource(
                    when (state.stage) {
                        LockStage.SETUP -> R.string.lock_setup_title
                        LockStage.CONFIRM -> R.string.lock_confirm_title
                        else -> R.string.lock_title
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))
            PinDots(filled = state.entry.length, total = LockViewModel.PIN_LENGTH)

            // Fixed height so showing an error never shifts the keypad under the
            // user's thumb mid-entry.
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .padding(top = 12.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                if (state.error != null) {
                    Text(
                        text = stringResource(
                            when (state.error) {
                                LockError.MISMATCH -> R.string.lock_mismatch
                                else -> R.string.lock_wrong_pin
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(Modifier.weight(0.6f))

            Keypad(
                onDigit = viewModel::press,
                onBackspace = viewModel::backspace,
            )

            Spacer(Modifier.height(16.dp))

            if (state.stage == LockStage.ENTER && state.biometricEnabled &&
                canUseBiometrics(context)
            ) {
                TextButton(
                    onClick = { promptBiometric(activity, context, viewModel::unlockViaBiometric) },
                ) {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.lock_use_biometric))
                }
            }
            Spacer(Modifier.weight(0.4f))
        }
    }
}

@Composable
private fun PinDots(filled: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(total) { index ->
            val isFilled = index < filled
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .then(
                        if (isFilled) {
                            Modifier.background(MaterialTheme.colorScheme.primary)
                        } else {
                            Modifier.border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                            )
                        }
                    )
            )
        }
    }
}

@Composable
private fun Keypad(onDigit: (Char) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "<"),
    )
    Column(
        modifier = Modifier.widthIn(max = 280.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { key ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        when (key) {
                            "" -> Spacer(Modifier.size(64.dp))
                            "<" -> KeyButton(onClick = onBackspace) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.Backspace,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            else -> KeyButton(onClick = { onDigit(key[0]) }) {
                                Text(
                                    text = key,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

private fun canUseBiometrics(context: Context): Boolean =
    BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
        BiometricManager.BIOMETRIC_SUCCESS

private fun promptBiometric(
    activity: FragmentActivity,
    context: Context,
    onSuccess: () -> Unit,
) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(context),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        },
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.lock_biometric_title))
            .setSubtitle(context.getString(R.string.lock_biometric_subtitle))
            .setNegativeButtonText(context.getString(R.string.lock_title))
            .build()
    )
}
