// ================================================================
// NAVIGATIONCOMPONENTS.KT - COMPOSANTS DE NAVIGATION ARKA
// ================================================================

package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ui.theme.*

/**
 * BARRE DE NAVIGATION SUPÉRIEURE ARKA
 */

/**
 * Barre de navigation supérieure personnalisée pour Arka
 */
@Composable
fun ArkaTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: ImageVector? = Icons.Default.ArrowBack,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = MaterialTheme.colors.onSurface,
    elevation: androidx.compose.ui.unit.Dp = 4.dp
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = ArkaTextStyles.helpText,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = if (navigationIcon != null && onNavigationClick != null) {
            {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = "Navigation",
                        tint = contentColor
                    )
                }
            }
        } else null,
        actions = actions,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        elevation = elevation,
        modifier = modifier
    )
}

/**
 * FIL D'ARIANE (BREADCRUMB)
 */

/**
 * Élément de fil d'Ariane
 */
data class BreadcrumbItem(
    val text: String,
    val onClick: (() -> Unit)? = null
)

/**
 * Composant de fil d'Ariane pour Arka
 */
@Composable
fun ArkaBreadcrumb(
    items: List<BreadcrumbItem>,
    modifier: Modifier = Modifier,
    separator: ImageVector = Icons.Default.ChevronRight,
    maxItems: Int = 4
) {
    if (items.isNotEmpty()) {
        LazyRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayItems = if (items.size > maxItems) {
                listOf(items.first()) + listOf(BreadcrumbItem("...")) + items.takeLast(maxItems - 2)
            } else {
                items
            }

            items(displayItems.size) { index ->
                val item = displayItems[index]
                val isLast = index == displayItems.lastIndex

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Texte cliquable ou non
                    if (item.onClick != null && !isLast) {
                        Text(
                            text = item.text,
                            style = ArkaTextStyles.breadcrumb,
                            color = MaterialTheme.colors.primary,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { item.onClick.invoke() }
                        )
                    } else {
                        Text(
                            text = item.text,
                            style = ArkaTextStyles.breadcrumb,
                            color = if (isLast) {
                                MaterialTheme.colors.primary
                            } else {
                                MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            },
                            fontWeight = if (isLast) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Séparateur
                    if (!isLast) {
                        Icon(
                            imageVector = separator,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * NAVIGATION LATÉRALE
 */

/**
 * Élément de navigation latérale
 */
data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val badge: String? = null,
    val isSelected: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Panneau de navigation latérale pour Arka
 */
@Composable
fun ArkaSideNavigation(
    items: List<NavigationItem>,
    modifier: Modifier = Modifier,
    header: @Composable (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp),
        color = MaterialTheme.colors.surface,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // En-tête
            header?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    it()
                }
                Divider()
            }

            // Éléments de navigation
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                items.forEach { item ->
                    NavigationItemRow(
                        item = item,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Pied de page
            footer?.let {
                Divider()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    it()
                }
            }
        }
    }
}

/**
 * Ligne d'élément de navigation
 */
@Composable
private fun NavigationItemRow(
    item: NavigationItem,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clickable { item.onClick() },
        color = if (item.isSelected) {
            MaterialTheme.colors.primary.copy(alpha = 0.12f)
        } else {
            Color.Transparent
        },
        shape = ArkaComponentShapes.cardSmall
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (item.isSelected) {
                    MaterialTheme.colors.primary
                } else {
                    MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                }
            )

            Text(
                text = item.label,
                style = ArkaTextStyles.navigation,
                color = if (item.isSelected) {
                    MaterialTheme.colors.primary
                } else {
                    MaterialTheme.colors.onSurface
                },
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Badge
            item.badge?.let { badgeText ->
                StatusBadge(
                    text = badgeText,
                    backgroundColor = MaterialTheme.colors.error,
                    textColor = MaterialTheme.colors.onError
                )
            }
        }
    }
}

/**
 * NAVIGATION PAR ONGLETS
 */

/**
 * Onglet personnalisé pour Arka
 */
data class ArkaTab(
    val text: String,
    val icon: ImageVector? = null,
    val badge: String? = null,
    val enabled: Boolean = true
)

/**
 * Barre d'onglets personnalisée
 */
@Composable
fun ArkaTabRow(
    tabs: List<ArkaTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = MaterialTheme.colors.primary,
    scrollable: Boolean = false
) {
    if (scrollable) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = modifier,
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffsetCompat(tabPositions[selectedTabIndex]),
                    color = contentColor
                )
            }
        ) {
            ArkaTabContent(tabs, selectedTabIndex, onTabSelected)
        }
    } else {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = modifier,
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffsetCompat(tabPositions[selectedTabIndex]),
                    color = contentColor
                )
            }
        ) {
            ArkaTabContent(tabs, selectedTabIndex, onTabSelected)
        }
    }
}

/**
 * Contenu des onglets
 */
@Composable
private fun ArkaTabContent(
    tabs: List<ArkaTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    tabs.forEachIndexed { index, tab ->
        Tab(
            selected = selectedTabIndex == index,
            onClick = { if (tab.enabled) onTabSelected(index) },
            enabled = tab.enabled,
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tab.icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = tab.text,
                        style = ArkaTextStyles.navigation
                    )

                    tab.badge?.let { badgeText ->
                        StatusBadge(
                            text = badgeText,
                            backgroundColor = MaterialTheme.colors.error,
                            textColor = MaterialTheme.colors.onError
                        )
                    }
                }
            }
        )
    }
}

/**
 * BOTTOM SHEET DE NAVIGATION
 */

/**
 * Bottom sheet pour navigation mobile
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ArkaNavigationBottomSheet(
    bottomSheetState: BottomSheetState,
    items: List<NavigationItem>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    BottomSheetScaffold(
        scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = bottomSheetState
        ),
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Handle de drag
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                            shape = ArkaComponentShapes.cardSmall
                        )
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Titre
                Text(
                    text = "Navigation",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Éléments de navigation
                items.forEach { item ->
                    NavigationItemRow(
                        item = item.copy(
                            onClick = {
                                item.onClick()
                                scope.launch {
                                    bottomSheetState.collapse()
                                }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        sheetShape = ArkaComponentShapes.cardLarge,
        modifier = modifier
    ) {
        content()
    }
}

/**
 * FAB pour ouvrir la navigation
 */
@Composable
fun NavigationFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Menu
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Ouvrir navigation"
        )
    }
}

/**
 * UTILITAIRES DE NAVIGATION
 */

/**
 * Créateur d'éléments de navigation standard pour Arka
 */
object ArkaNavigationItems {

    fun home(isSelected: Boolean = false, onClick: () -> Unit) = NavigationItem(
        label = "Accueil",
        icon = Icons.Default.Home,
        route = "home",
        isSelected = isSelected,
        onClick = onClick
    )

    fun files(isSelected: Boolean = false, onClick: () -> Unit) = NavigationItem(
        label = "Mes fichiers",
        icon = Icons.Default.Folder,
        route = "files",
        isSelected = isSelected,
        onClick = onClick
    )

    fun family(isSelected: Boolean = false, badge: String? = null, onClick: () -> Unit) = NavigationItem(
        label = "Famille",
        icon = Icons.Default.Group,
        route = "family",
        badge = badge,
        isSelected = isSelected,
        onClick = onClick
    )

    fun permissions(isSelected: Boolean = false, badge: String? = null, onClick: () -> Unit) = NavigationItem(
        label = "Permissions",
        icon = Icons.Default.Security,
        route = "permissions",
        badge = badge,
        isSelected = isSelected,
        onClick = onClick
    )

    fun alerts(isSelected: Boolean = false, badge: String? = null, onClick: () -> Unit) = NavigationItem(
        label = "Alertes",
        icon = Icons.Default.Notifications,
        route = "alerts",
        badge = badge,
        isSelected = isSelected,
        onClick = onClick
    )

    fun settings(isSelected: Boolean = false, onClick: () -> Unit) = NavigationItem(
        label = "Paramètres",
        icon = Icons.Default.Settings,
        route = "settings",
        isSelected = isSelected,
        onClick = onClick
    )
}

/**
 * Extension pour compatibilité tabIndicatorOffset
 * Remplace la fonction dépréciée tabIndicatorOffset
 */
private fun Modifier.tabIndicatorOffsetCompat(
    currentTabPosition: TabPosition
): Modifier = this.then(
    Modifier
        .fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = currentTabPosition.left)
        .width(currentTabPosition.width)
)