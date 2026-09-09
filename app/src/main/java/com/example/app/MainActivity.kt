package com.example.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.app.ui.theme.APPTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class TodoItem(
    val text: String,
    val category: String,
    val done: Boolean = false
)

private const val API_BASE_URL = "http://10.0.2.2:8000"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            APPTheme {
                AppRoot()
            }
        }
    }
}

private suspend fun loginUser(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
    try {
        val url = URL("$API_BASE_URL/login")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
        }

        val payload = JSONObject().apply {
            put("email", email)
            put("password", password)
        }.toString()

        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(payload)
            writer.flush()
        }

        val responseCode = connection.responseCode
        val responseText = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use(BufferedReader::readText)
        } else {
            connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
        }

        if (responseCode in 200..299) {
            val json = JSONObject(responseText)
            val token = json.optString("token", "")
            if (token.isNotBlank()) {
                Result.success(token)
            } else {
                Result.failure(IllegalStateException("No se recibió token válido"))
            }
        } else {
            val message = try {
                JSONObject(responseText).optString("detail", "Credenciales inválidas")
            } catch (_: Exception) {
                "Credenciales inválidas"
            }
            Result.failure(IllegalStateException(message))
        }
    } catch (exception: Exception) {
        Log.e("AppLogin", "Login failed", exception)
        Result.failure(exception)
    }
}

@Composable
fun AppRoot() {
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("admin@app.com") }
    var password by rememberSaveable { mutableStateOf("123456") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var loginError by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (!isLoggedIn) {
        LoginScreen(
            email = email,
            password = password,
            isLoading = isLoading,
            errorMessage = loginError,
            onEmailChange = { email = it },
            onPasswordChange = { password = it },
            onLoginClick = {
                if (email.isBlank() || password.isBlank()) {
                    loginError = "Completa correo y contraseña"
                    return@LoginScreen
                }

                isLoading = true
                loginError = null

                scope.launch {
                    val result = loginUser(email.trim(), password.trim())
                    result.onSuccess {
                        isLoggedIn = true
                        isLoading = false
                    }.onFailure {
                        loginError = it.message ?: "Error al iniciar sesión"
                        isLoading = false
                    }
                }
            }
        )
    } else {
        AppScreen(
            onLogout = {
                isLoggedIn = false
                email = "admin@app.com"
                password = "123456"
                loginError = null
            }
        )
    }
}

@Composable
fun LoginScreen(
    email: String,
    password: String,
    isLoading: Boolean,
    errorMessage: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Inicia sesión",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Correo electrónico") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onLoginClick,
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Entrando..." else "Entrar")
            }
        }
    }
}

@Composable
fun AppScreen(modifier: Modifier = Modifier, onLogout: (() -> Unit)? = null) {
    var taskInput by rememberSaveable { mutableStateOf("") }
    val tasks = remember {
        mutableStateListOf(
            TodoItem("Diseñar la pantalla principal", "Diseño", false),
            TodoItem("Revisar requisitos del backend", "Trabajo", true),
            TodoItem("Preparar demo final", "Producto", false),
            TodoItem("Subir cambios a GitHub", "Operación", true)
        )
    }

    val completed = tasks.count { it.done }
    val pending = tasks.size - completed

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF6C63FF),
                                        Color(0xFF8B5CF6),
                                        Color(0xFF4F46E5)
                                    )
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "¡Buen trabajo!",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Panel de productividad",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "8 tareas activas esta semana",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(label = "Completadas", value = completed.toString(), modifier = Modifier.weight(1f))
                    StatCard(label = "Pendientes", value = pending.toString(), modifier = Modifier.weight(1f))
                }
            }

            item {
                Text(
                    text = "Tareas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(tasks, key = { _, item -> item.text }) { index, task ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = task.done,
                            onCheckedChange = { checked ->
                                tasks[index] = task.copy(done = checked)
                            }
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.text,
                                style = MaterialTheme.typography.bodyLarge,
                                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                                fontWeight = if (task.done) FontWeight.Normal else FontWeight.Medium
                            )
                            Text(
                                text = task.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Agregar tarea",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (onLogout != null) {
                        Button(onClick = onLogout) {
                            Text("Salir")
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = taskInput,
                    onValueChange = { taskInput = it },
                    label = { Text("Escribe una tarea") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Button(
                    onClick = {
                        val trimmed = taskInput.trim()
                        if (trimmed.isNotEmpty()) {
                            tasks.add(0, TodoItem(trimmed, "Nueva", false))
                            taskInput = ""
                        }
                    },
                    enabled = taskInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Agregar")
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppScreenPreview() {
    APPTheme {
        AppScreen()
    }
}