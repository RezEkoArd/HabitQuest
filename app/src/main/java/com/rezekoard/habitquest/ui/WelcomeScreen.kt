package com.rezekoard.habitquest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rezekoard.habitquest.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(onContinue: suspend (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val submit = {
        if (name.isNotBlank() && !saving) {
            saving = true
            error = null
            scope.launch {
                try { onContinue(name.trim()) }
                catch (exception: CancellationException) { throw exception }
                catch (_: Exception) { error = "Nama belum tersimpan. Silakan coba lagi." }
                finally { saving = false }
            }
        }
    }
    Box(Modifier.fillMaxSize().background(Canvas).safeDrawingPadding().imePadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(painterResource(R.drawable.ic_sprout), null, tint = Forest)
                Text("habitquest", style = MaterialTheme.typography.titleLarge, color = Ink)
            }
            Spacer(Modifier.height(44.dp))
            Box(Modifier.size(88.dp).background(SoftGreen, RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_sprout), null, Modifier.size(48.dp), tint = Forest)
            }
            Spacer(Modifier.height(25.dp))
            Text("Kita kenalan dulu.", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center, color = Ink)
            Spacer(Modifier.height(12.dp))
            Text("Kebiasaan baik dimulai dari langkah kecil.\nKamu ingin dipanggil siapa?", color = Muted, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(26.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(30); error = null },
                label = { Text("Nama panggilan") }, placeholder = { Text("Contoh: Rezek") },
                singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                isError = error != null,
                supportingText = { Text(error ?: "Maksimal 30 karakter.") },
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = { submit() }, enabled = name.isNotBlank() && !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp)) {
                Text(if (saving) "Menyimpan…" else "Mulai perjalanan")
            }
            Spacer(Modifier.height(24.dp))
            Text("Tersimpan di perangkatmu. Tanpa akun.\nKamu bisa mulai dengan satu habit saja.", style = MaterialTheme.typography.bodySmall, color = Muted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun StartupScreen(error: String? = null, onRetry: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(Canvas).safeDrawingPadding().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painterResource(R.drawable.ic_sprout), null, Modifier.size(48.dp), tint = Forest)
        Spacer(Modifier.height(20.dp))
        if (error == null) {
            CircularProgressIndicator(color = Forest, modifier = Modifier.size(28.dp))
        } else {
            Text(error, color = Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(15.dp))
            Button(onClick = onRetry) { Text("Coba lagi") }
        }
    }
}
