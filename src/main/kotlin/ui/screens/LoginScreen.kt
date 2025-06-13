// ================================================================
// LOGINSCREEN.KT - ÉCRAN DE CONNEXION ARKA
// ================================================================

package ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import controllers.AuthController
import controllers.LoginRequest
import controllers.RegisterRequest
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Écran de connexion principal d'Arka
 *
 * Fonctionnalités:
 * - Connexion avec email/mot de passe
 * - Basculement vers inscription
 * - Gestion des erreurs
 * - Interface responsive
 * - Validation des champs
 */
@Composable
fun LoginScreen(
    authController: AuthController,
    onLoginSuccess: () -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Animation de transition entre login et register
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            elevation = 12.dp,
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo et titre Arka
                ArkaLogo()

                Spacer(modifier = Modifier.height(32.dp))

                // Onglets Login/Register
                LoginRegisterTabs(
                    isLoginMode = isLoginMode,
                    onModeChange = {
                        isLoginMode = it
                        errorMessage = null
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Contenu selon le mode
                if (isLoginMode) {
                    LoginForm(
                        authController = authController,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onLoadingChange = { isLoading = it },
                        onErrorChange = { errorMessage = it },
                        onLoginSuccess = onLoginSuccess
                    )
                } else {
                    RegisterForm(
                        authController = authController,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onLoadingChange = { isLoading = it },
                        onErrorChange = { errorMessage = it },
                        onRegisterSuccess = {
                            isLoginMode = true
                            errorMessage = "Compte créé avec succès ! Vous pouvez maintenant vous connecter."
                        }
                    )
                }
            }
        }
    }
}

/**
 * Logo et présentation d'Arka
 */
@Composable
private fun ArkaLogo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Icône Arka (remplacer par le logo réel si disponible)
        Icon(
            imageVector = Icons.Default.FamilyRestroom,
            contentDescription = "Logo Arka",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colors.primary
        )

        Text(
            text = "Arka",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )

        Text(
            text = "Gestion de fichiers familiaux",
            fontSize = 16.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Text(
            text = "Organisez, partagez et sécurisez vos documents en famille",
            fontSize = 14.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Onglets de basculement Login/Register
 */
@Composable
private fun LoginRegisterTabs(
    isLoginMode: Boolean,
    onModeChange: (Boolean) -> Unit
) {
    TabRow(
        selectedTabIndex = if (isLoginMode) 0 else 1,
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.primary,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.tabIndicatorOffset(tabPositions[if (isLoginMode) 0 else 1]),
                color = MaterialTheme.colors.primary
            )
        }
    ) {
        Tab(
            selected = isLoginMode,
            onClick = { onModeChange(true) },
            text = {
                Text(
                    text = "Connexion",
                    style = ArkaTextStyles.navigation
                )
            }
        )

        Tab(
            selected = !isLoginMode,
            onClick = { onModeChange(false) },
            text = {
                Text(
                    text = "Inscription",
                    style = ArkaTextStyles.navigation
                )
            }
        )
    }
}

/**
 * Formulaire de connexion
 */
@Composable
private fun LoginForm(
    authController: AuthController,
    isLoading: Boolean,
    errorMessage: String?,
    onLoadingChange: (Boolean) -> Unit,
    onErrorChange: (String?) -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Champ email
        ArkaTextField(
            value = email,
            onValueChange = {
                email = it
                onErrorChange(null)
            },
            label = "Adresse email",
            leadingIcon = Icons.Default.Email,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            isError = errorMessage != null
        )

        // Champ mot de passe
        ArkaPasswordField(
            value = password,
            onValueChange = {
                password = it
                onErrorChange(null)
            },
            label = "Mot de passe",
            isError = errorMessage != null,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (email.isNotBlank() && password.isNotBlank()) {
                        scope.launch {
                            performLogin(
                                authController = authController,
                                email = email,
                                password = password,
                                onLoadingChange = onLoadingChange,
                                onErrorChange = onErrorChange,
                                onLoginSuccess = onLoginSuccess
                            )
                        }
                    }
                }
            )
        )

        // Message d'erreur global
        if (errorMessage != null) {
            ErrorMessage(
                message = errorMessage,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bouton de connexion
        ArkaButton(
            text = "Se connecter",
            onClick = {
                scope.launch {
                    performLogin(
                        authController = authController,
                        email = email,
                        password = password,
                        onLoadingChange = onLoadingChange,
                        onErrorChange = onErrorChange,
                        onLoginSuccess = onLoginSuccess
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            loading = isLoading,
            icon = Icons.Default.Login
        )

        // Lien mot de passe oublié
        TextButton(
            onClick = { /* TODO: Implémenter mot de passe oublié */ },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Mot de passe oublié ?",
                style = ArkaTextStyles.link,
                color = MaterialTheme.colors.primary
            )
        }
    }
}

/**
 * Formulaire d'inscription
 */
@Composable
private fun RegisterForm(
    authController: AuthController,
    isLoading: Boolean,
    errorMessage: String?,
    onLoadingChange: (Boolean) -> Unit,
    onErrorChange: (String?) -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var acceptTerms by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val genderOptions = listOf("M" to "Homme", "F" to "Femme", "Autre" to "Autre")

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Champ prénom
        ArkaTextField(
            value = firstName,
            onValueChange = {
                firstName = it
                onErrorChange(null)
            },
            label = "Prénom",
            leadingIcon = Icons.Default.Person,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        // Champ email
        ArkaTextField(
            value = email,
            onValueChange = {
                email = it
                onErrorChange(null)
            },
            label = "Adresse email",
            leadingIcon = Icons.Default.Email,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        // Sélection du genre
        var genderExpanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = !genderExpanded }
        ) {
            ArkaTextField(
                value = genderOptions.find { it.first == gender }?.second ?: "",
                onValueChange = { },
                readOnly = true,
                label = "Genre",
                leadingIcon = Icons.Default.Wc,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded)
                }
            )

            ExposedDropdownMenu(
                expanded = genderExpanded,
                onDismissRequest = { genderExpanded = false }
            ) {
                genderOptions.forEach { option ->
                    DropdownMenuItem(
                        onClick = {
                            gender = option.first
                            genderExpanded = false
                            onErrorChange(null)
                        }
                    ) {
                        Text(option.second)
                    }
                }
            }
        }

        // Champ mot de passe
        ArkaPasswordField(
            value = password,
            onValueChange = {
                password = it
                onErrorChange(null)
            },
            label = "Mot de passe",
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        // Confirmation mot de passe
        ArkaPasswordField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                onErrorChange(null)
            },
            label = "Confirmer le mot de passe",
            isError = password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword,
            errorMessage = if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
                "Les mots de passe ne correspondent pas"
            } else null
        )

        // Acceptation des conditions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = acceptTerms,
                onCheckedChange = { acceptTerms = it }
            )

            Text(
                text = "J'accepte les conditions d'utilisation",
                style = ArkaTextStyles.cardDescription,
                color = MaterialTheme.colors.onSurface
            )
        }

        // Message d'erreur global
        if (errorMessage != null && !errorMessage.contains("succès")) {
            ErrorMessage(
                message = errorMessage,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (errorMessage != null && errorMessage.contains("succès")) {
            ArkaCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colors.arka.success.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colors.arka.success,
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = errorMessage,
                        style = ArkaTextStyles.cardDescription,
                        color = MaterialTheme.colors.arka.success
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bouton d'inscription
        ArkaButton(
            text = "Créer le compte",
            onClick = {
                scope.launch {
                    performRegister(
                        authController = authController,
                        firstName = firstName,
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
                        gender = gender,
                        acceptTerms = acceptTerms,
                        onLoadingChange = onLoadingChange,
                        onErrorChange = onErrorChange,
                        onRegisterSuccess = onRegisterSuccess
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading &&
                    firstName.isNotBlank() &&
                    email.isNotBlank() &&
                    password.isNotBlank() &&
                    confirmPassword.isNotBlank() &&
                    password == confirmPassword &&
                    gender.isNotBlank() &&
                    acceptTerms,
            loading = isLoading,
            icon = Icons.Default.PersonAdd
        )
    }
}

/**
 * Logique de connexion
 */
private suspend fun performLogin(
    authController: AuthController,
    email: String,
    password: String,
    onLoadingChange: (Boolean) -> Unit,
    onErrorChange: (String?) -> Unit,
    onLoginSuccess: () -> Unit
) {
    onLoadingChange(true)

    val result = authController.login(LoginRequest(email.trim(), password))

    when (result) {
        is AuthController.AuthResult.Success -> {
            onLoginSuccess()
        }
        is AuthController.AuthResult.Error -> {
            onErrorChange(result.message)
        }
    }

    onLoadingChange(false)
}

/**
 * Logique d'inscription
 */
private suspend fun performRegister(
    authController: AuthController,
    firstName: String,
    email: String,
    password: String,
    confirmPassword: String,
    gender: String,
    acceptTerms: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    onErrorChange: (String?) -> Unit,
    onRegisterSuccess: () -> Unit
) {
    // Validations
    if (password != confirmPassword) {
        onErrorChange("Les mots de passe ne correspondent pas")
        return
    }

    if (!acceptTerms) {
        onErrorChange("Vous devez accepter les conditions d'utilisation")
        return
    }

    onLoadingChange(true)

    val result = authController.register(
        RegisterRequest(
            firstName = firstName.trim(),
            email = email.trim().lowercase(),
            password = password,
            dateOfBirth = LocalDate.of(1990, 1, 1), // Date par défaut
            gender = gender,
            familyId = 1 // TODO: Gérer la sélection/création de famille
        )
    )

    when (result) {
        is AuthController.AuthResult.Success -> {
            onRegisterSuccess()
        }
        is AuthController.AuthResult.Error -> {
            onErrorChange(result.message)
        }
    }

    onLoadingChange(false)