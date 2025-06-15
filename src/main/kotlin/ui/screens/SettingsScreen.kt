// ================================================================
// SETTINGSSCREEN.KT - CONFIGURATION UTILISATEUR
// ================================================================

package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import controllers.*
import kotlinx.coroutines.launch
import ui.components.*
import ui.theme.*

/**
 * Écran de configuration utilisateur
 *
 * Fonctionnalités:
 * - Gestion du profil utilisateur
 * - Modification du mot de passe
 * - Paramètres de notifications
 * - Préférences de l'interface
 * - Paramètres de sécurité
 * - Gestion de la famille (pour les parents)
 */
@Composable
fun SettingsScreen(
    authController: AuthController,
    familyMemberController: FamilyMemberController,
    familyController: FamilyController,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser = authController.getCurrentUser()

    // États de l'écran
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    // États des paramètres
    var notificationsEnabled by remember { mutableStateOf(true) }
    var emailNotifications by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(false) }
    var autoBackup by remember { mutableStateOf(true) }
    var twoFactorAuth by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // En-tête
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Paramètres",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section Profil
                item {
                    currentUser?.let { user ->
                        ProfileSection(
                            user = user,
                            onUpdateProfile = { updatedUser ->
                                scope.launch {
                                    isLoading = true
                                    try {
                                        val result = familyMemberController.updateFamilyMember(updatedUser)
                                        when (result) {
                                            is FamilyMemberController.FamilyMemberResult.Success -> {
                                                successMessage = "Profil mis à jour avec succès"
                                            }
                                            is FamilyMemberController.FamilyMemberResult.Error -> {
                                                errorMessage = result.message
                                            }
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Erreur lors de la mise à jour: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        )
                    }
                }

                // Section Sécurité
                item {
                    SecuritySection(
                        twoFactorEnabled = twoFactorAuth,
                        onChangePassword = { showChangePasswordDialog = true },
                        onToggleTwoFactor = { twoFactorAuth = it }
                    )
                }

                // Section Notifications
                item {
                    NotificationSection(
                        notificationsEnabled = notificationsEnabled,
                        emailNotifications = emailNotifications,
                        onToggleNotifications = { notificationsEnabled = it },
                        onToggleEmailNotifications = { emailNotifications = it }
                    )
                }

                // Section Interface
                item {
                    InterfaceSection(
                        darkMode = darkMode,
                        autoBackup = autoBackup,
                        onToggleDarkMode = { darkMode = it },
                        onToggleAutoBackup = { autoBackup = it }
                    )
                }

                // Section Famille (pour les parents uniquement)
                item {
                    currentUser?.let { user ->
                        if (user.role == RoleFamille.PARENT) {
                            FamilyManagementSection(
                                familyController = familyController,
                                familyMemberController = familyMemberController,
                                currentUser = user
                            )
                        }
                    }
                }

                // Section Actions dangereuses
                item {
                    DangerZoneSection(
                        onDeleteAccount = { showDeleteAccountDialog = true },
                        onLogout = onLogout
                    )
                }
            }
        }

        // Messages de feedback
        successMessage?.let { message ->
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(3000)
                successMessage = null
            }

            Snackbar(
                modifier = Modifier.padding(16.dp),
                backgroundColor = Color.Green.copy(alpha = 0.8f)
            ) {
                Text(text = message, color = Color.White)
            }
        }

        errorMessage?.let { message ->
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(5000)
                errorMessage = null
            }

            Snackbar(
                modifier = Modifier.padding(16.dp),
                backgroundColor = Color.Red.copy(alpha = 0.8f)
            ) {
                Text(text = message, color = Color.White)
            }
        }

        // Dialogs
        if (showChangePasswordDialog) {
            ChangePasswordDialog(
                authController = authController,
                onDismiss = { showChangePasswordDialog = false },
                onSuccess = {
                    showChangePasswordDialog = false
                    successMessage = "Mot de passe modifié avec succès"
                },
                onError = { error ->
                    errorMessage = error
                }
            )
        }

        if (showDeleteAccountDialog) {
            DeleteAccountDialog(
                onDismiss = { showDeleteAccountDialog = false },
                onConfirm = {
                    // Logique de suppression de compte
                    showDeleteAccountDialog = false
                    onLogout()
                }
            )
        }
    }
}

/**
 * Section de gestion du profil
 */
@Composable
private fun ProfileSection(
    user: MembreFamille,
    onUpdateProfile: (MembreFamille) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedPrenom by remember { mutableStateOf(user.prenom) }
    var editedNom by remember { mutableStateOf(user.nom) }
    var editedEmail by remember { mutableStateOf(user.email) }

    SettingsSection(
        title = "Profil",
        icon = Icons.Default.Person
    ) {
        if (isEditing) {
            // Mode édition
            OutlinedTextField(
                value = editedPrenom,
                onValueChange = { editedPrenom = it },
                label = { Text("Prénom") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = editedNom,
                onValueChange = { editedNom = it },
                label = { Text("Nom") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = editedEmail,
                onValueChange = { editedEmail = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Button(
                    onClick = {
                        onUpdateProfile(
                            user.copy(
                                prenom = editedPrenom,
                                nom = editedNom,
                                email = editedEmail
                            )
                        )
                        isEditing = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sauvegarder")
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = {
                        editedPrenom = user.prenom
                        editedNom = user.nom
                        editedEmail = user.email
                        isEditing = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Annuler")
                }
            }
        } else {
            // Mode affichage
            ProfileInfoRow("Prénom", user.prenom)
            ProfileInfoRow("Nom", user.nom)
            ProfileInfoRow("Email", user.email)
            ProfileInfoRow("Rôle", user.role.name.lowercase().replaceFirstChar { it.uppercase() })

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { isEditing = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Modifier")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Modifier le profil")
            }
        }
    }
}

/**
 * Section de sécurité
 */
@Composable
private fun SecuritySection(
    twoFactorEnabled: Boolean,
    onChangePassword: () -> Unit,
    onToggleTwoFactor: (Boolean) -> Unit
) {
    SettingsSection(
        title = "Sécurité",
        icon = Icons.Default.Security
    ) {
        SettingsItem(
            title = "Changer le mot de passe",
            subtitle = "Mettre à jour votre mot de passe",
            icon = Icons.Default.Lock,
            onClick = onChangePassword
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        SettingsToggle(
            title = "Authentification à deux facteurs",
            subtitle = "Sécurité renforcée avec 2FA",
            checked = twoFactorEnabled,
            onCheckedChange = onToggleTwoFactor
        )
    }
}

/**
 * Section des notifications
 */
@Composable
private fun NotificationSection(
    notificationsEnabled: Boolean,
    emailNotifications: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleEmailNotifications: (Boolean) -> Unit
) {
    SettingsSection(
        title = "Notifications",
        icon = Icons.Default.Notifications
    ) {
        SettingsToggle(
            title = "Notifications push",
            subtitle = "Recevoir des notifications dans l'application",
            checked = notificationsEnabled,
            onCheckedChange = onToggleNotifications
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        SettingsToggle(
            title = "Notifications email",
            subtitle = "Recevoir des notifications par email",
            checked = emailNotifications,
            onCheckedChange = onToggleEmailNotifications,
            enabled = notificationsEnabled
        )
    }
}

/**
 * Section de l'interface
 */
@Composable
private fun InterfaceSection(
    darkMode: Boolean,
    autoBackup: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleAutoBackup: (Boolean) -> Unit
) {
    SettingsSection(
        title = "Interface",
        icon = Icons.Default.Palette
    ) {
        SettingsToggle(
            title = "Mode sombre",
            subtitle = "Interface avec couleurs sombres",
            checked = darkMode,
            onCheckedChange = onToggleDarkMode
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        SettingsToggle(
            title = "Sauvegarde automatique",
            subtitle = "Sauvegarder automatiquement vos modifications",
            checked = autoBackup,
            onCheckedChange = onToggleAutoBackup
        )
    }
}

/**
 * Section de gestion familiale (pour les parents)
 */
@Composable
private fun FamilyManagementSection(
    familyController: FamilyController,
    familyMemberController: FamilyMemberController,
    currentUser: MembreFamille
) {
    SettingsSection(
        title = "Gestion familiale",
        icon = Icons.Default.FamilyRestroom
    ) {
        SettingsItem(
            title = "Gérer les membres",
            subtitle = "Ajouter, modifier ou supprimer des membres",
            icon = Icons.Default.People,
            onClick = { /* Navigation vers gestion des membres */ }
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        SettingsItem(
            title = "Paramètres de la famille",
            subtitle = "Configuration globale de la famille",
            icon = Icons.Default.Settings,
            onClick = { /* Navigation vers paramètres famille */ }
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        SettingsItem(
            title = "Codes d'invitation",
            subtitle = "Générer des codes pour inviter des membres",
            icon = Icons.Default.QrCode,
            onClick = { /* Navigation vers codes d'invitation */ }
        )
    }
}

/**
 * Section des actions dangereuses
 */
@Composable
private fun DangerZoneSection(
    onDeleteAccount: () -> Unit,
    onLogout: () -> Unit
) {
    SettingsSection(
        title = "Zone dangereuse",
        icon = Icons.Default.Warning,
        titleColor = Color.Red
    ) {
        SettingsItem(
            title = "Se déconnecter",
            subtitle = "Fermer la session actuelle",
            icon = Icons.Default.ExitToApp,
            onClick = onLogout
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        SettingsItem(
            title = "Supprimer le compte",
            subtitle = "Supprimer définitivement votre compte",
            icon = Icons.Default.DeleteForever,
            titleColor = Color.Red,
            onClick = onDeleteAccount
        )
    }
}

/**
 * Composant de section de paramètres
 */
@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    titleColor: Color = MaterialTheme.colors.onSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = titleColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

/**
 * Item de paramètre cliquable
 */
@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    titleColor: Color = MaterialTheme.colors.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = false,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = titleColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "Accéder",
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
        )
    }
}

/**
 * Toggle de paramètre
 */
@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = if (enabled) 0.6f else 0.4f)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

/**
 * Ligne d'information de profil
 */
@Composable
private fun ProfileInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Dialog de changement de mot de passe
 */
@Composable
private fun ChangePasswordDialog(
    authController: AuthController,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isChanging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Changer le mot de passe") },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Mot de passe actuel") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nouveau mot de passe") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmer le mot de passe") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword != confirmPassword) {
                        onError("Les mots de passe ne correspondent pas")
                        return@Button
                    }

                    scope.launch {
                        isChanging = true
                        try {
                            val result = authController.changePassword(currentPassword, newPassword)
                            when (result) {
                                is AuthController.AuthResult.Success -> onSuccess()
                                is AuthController.AuthResult.Error -> onError(result.message)
                            }
                        } catch (e: Exception) {
                            onError("Erreur: ${e.message}")
                        } finally {
                            isChanging = false
                        }
                    }
                },
                enabled = !isChanging && currentPassword.isNotBlank() &&
                        newPassword.isNotBlank() && confirmPassword.isNotBlank()
            ) {
                if (isChanging) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Changer")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Dialog de suppression de compte
 */
@Composable
private fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = "Attention", tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Supprimer le compte", color = Color.Red)
            }
        },
        text = {
            Text("Cette action est irréversible. Toutes vos données seront définitivement supprimées.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
            ) {
                Text("Supprimer définitivement", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}