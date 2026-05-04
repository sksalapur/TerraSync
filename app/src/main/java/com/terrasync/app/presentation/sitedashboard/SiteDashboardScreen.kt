package com.terrasync.app.presentation.sitedashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.terrasync.app.domain.model.RiskLevel
import com.terrasync.app.domain.model.SoilNode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SiteDashboard screen — real-time view of all soil nodes under a site.
 *
 * Receives live updates from Firestore via [DashboardViewModel.uiState] (StateFlow
 * backed by a snapshot listener). A [LazyColumn] renders each node as a card with:
 * - Permeability class badge (color-coded Red/Amber/Green)
 * - Key soil parameters in a compact grid
 * - AI risk insights summary
 *
 * @param onAddNode    Navigate to AddNodeFormScreen.
 * @param onBack       Navigate back to SiteGateway.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SiteDashboardScreen(
    onAddNode : () -> Unit,
    onEditNode: (String) -> Unit = {},
    onBack    : () -> Unit,
    viewModel : DashboardViewModel = hiltViewModel(),
) {
    val uiState         by viewModel.uiState.collectAsStateWithLifecycle()
    val siteDisplayName by viewModel.siteDisplayName.collectAsStateWithLifecycle()
    val isAdmin         by viewModel.isAdmin.collectAsStateWithLifecycle()
    val selectionMode   by viewModel.selectionMode.collectAsStateWithLifecycle()
    val selectedNodeIds by viewModel.selectedNodeIds.collectAsStateWithLifecycle()
    val isDeleting      by viewModel.isDeletingNodes.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.siteDeletedEvent.collect {
            onBack()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    if (selectionMode) {
                        Text(
                            "${selectedNodeIds.size} Selected",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEAE1D9),
                            ),
                        )
                    } else {
                        Column {
                            Text(
                                "Site Dashboard",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFEAE1D9),
                                ),
                            )
                            Text(
                                siteDisplayName,
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9E8D82)),
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFFEAE1D9),
                            )
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFFD97040),
                            )
                        }
                    }
                },
                actions = {
                    if (selectionMode) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 16.dp).size(24.dp),
                                color = Color(0xFFE35D5D),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = { viewModel.deleteSelectedNodes() }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete selected",
                                    tint = Color(0xFFE35D5D),
                                )
                            }
                        }
                    } else {
                        // Node count badge
                        val count = (uiState as? DashboardUiState.Success)?.nodes?.size ?: 0
                        AnimatedVisibility(visible = count > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF3D3A36))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "$count nodes",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color      = Color(0xFFD97040),
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                        }

                        if (isAdmin) {
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "Admin Options",
                                        tint = Color(0xFFD97040),
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier.background(Color(0xFF252220))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Rename Site", color = Color(0xFFEAE1D9)) },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFD97040)) },
                                        onClick = {
                                            showMenu = false
                                            showRenameDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Site", color = Color(0xFFE35D5D)) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE35D5D)) },
                                        onClick = {
                                            showMenu = false
                                            showDeleteConfirm = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1C1B1B)),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick          = onAddNode,
                shape            = CircleShape,
                containerColor   = Color(0xFFD97040),
                contentColor     = Color.White,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add soil node")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1C1B1B), Color(0xFF221E1B)),
                        start  = Offset.Zero,
                        end    = Offset(0f, Float.POSITIVE_INFINITY),
                    )
                )
                .padding(innerPadding),
        ) {
            AnimatedContent(
                targetState   = uiState,
                label         = "dashboard_state",
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInVertically { it / 8 })
                        .togetherWith(fadeOut(tween(150)))
                },
            ) { state ->
                when (state) {
                    is DashboardUiState.Loading -> LoadingContent()
                    is DashboardUiState.Error   -> ErrorContent(state.message, onRetry = {})
                    is DashboardUiState.Success -> {
                        if (state.nodes.isEmpty()) {
                            EmptyContent(onAddNode)
                        } else {
                            NodeList(
                                nodes = state.nodes,
                                isAdmin = isAdmin,
                                selectionMode = selectionMode,
                                selectedNodeIds = selectedNodeIds,
                                onAddNode = onAddNode,
                                onEditNode = onEditNode,
                                onToggleSelection = viewModel::toggleNodeSelection,
                                onEnterSelectionMode = viewModel::enterSelectionMode,
                            )
                        }
                    }
                }
            }
        }
    }
    if (showRenameDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Site", color = Color(0xFFEAE1D9)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameSite(newName)
                    showRenameDialog = false
                }) {
                    Text("Rename", color = Color(0xFFD97040))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = Color(0xFF9E8D82))
                }
            },
            containerColor = Color(0xFF252220),
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Site", color = Color(0xFFE35D5D)) },
            text = { Text("Are you sure you want to delete this site? This action cannot be undone.", color = Color(0xFFEAE1D9)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSite()
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = Color(0xFFE35D5D))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color(0xFF9E8D82))
                }
            },
            containerColor = Color(0xFF252220),
        )
    }
}

// ── Node List ─────────────────────────────────────────────────────────────────

// ── Node List ─────────────────────────────────────────────────────────────────

@Composable
private fun NodeList(
    nodes: List<SoilNode>,
    isAdmin: Boolean,
    selectionMode: Boolean,
    selectedNodeIds: Set<String>,
    onAddNode: () -> Unit,
    onEditNode: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onEnterSelectionMode: () -> Unit,
) {
    LazyColumn(
        contentPadding     = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                "Bore-Log Records",
                style = MaterialTheme.typography.titleSmall.copy(
                    color      = Color(0xFF9E8D82),
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        itemsIndexed(nodes, key = { _, n -> n.id }) { index, node ->
            val isSelected = selectedNodeIds.contains(node.id)
            NodeCard(
                node = node,
                index = nodes.size - index,
                isAdmin = isAdmin,
                selectionMode = selectionMode,
                isSelected = isSelected,
                onClick = {
                    if (selectionMode) {
                        onToggleSelection(node.id)
                    } else if (isAdmin) {
                        onEditNode(node.id)
                    }
                },
                onLongClick = {
                    if (isAdmin && !selectionMode) {
                        onEnterSelectionMode()
                        onToggleSelection(node.id)
                    }
                }
            )
        }
        item { Spacer(Modifier.height(72.dp)) }  // FAB clearance
    }
}

// ── Node Card ─────────────────────────────────────────────────────────────────

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun NodeCard(
    node: SoilNode,
    index: Int,
    isAdmin: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val pClass = node.riskLevelEnum
    val tint   = Color(pClass.colorHex)
    val bgColor = if (isSelected) Color(0xFF3A2D25) else Color(0xFF252220)
    val borderColor = if (isSelected) Color(0xFFD97040) else tint.copy(alpha = 0.35f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (isAdmin) onLongClick else null
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── Header row ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Node index badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3D3A36)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "#$index",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFFD97040),
                        ),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        node.soilTypeLabel,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFFEAE1D9),
                        ),
                    )
                    Text(
                        "USCS: ${node.soilTypeSymbol}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9E8D82)),
                    )
                }
            }

            // Permeability badge or Selection Checkmark
            if (selectionMode) {
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color(0xFFD97040),
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .border(2.dp, Color(0xFF5E5550), CircleShape)
                    )
                }
            } else {
                RiskBadge(pClass = pClass, tint = tint)
            }
        }

        HorizontalDivider(color = Color(0xFF3A3330))

        // ── Grain size + index parameters grid ──────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ParamCell("D10", "${node.d10} mm", Modifier.weight(1f))
            ParamCell("D30", "${node.d30} mm", Modifier.weight(1f))
            ParamCell("D60", "${node.d60} mm", Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ParamCell("Cu",  "%.2f".format(node.coefficientOfUniformity), Modifier.weight(1f))
            ParamCell("Cc",  "%.2f".format(node.coefficientOfCurvature), Modifier.weight(1f))
            ParamCell("e",   "${node.voidRatio}", Modifier.weight(1f))
        }

        // Optional params (only if present)
        val optionals = buildList {
            node.moistureContent?.let { add("ω" to "$it %") }
            node.specificGravity?.let  { add("Gs" to "$it") }
            node.dryDensity?.let       { add("γd" to "$it kN/m³") }
        }
        if (optionals.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                optionals.forEach { (label, value) ->
                    ParamCell(label, value, Modifier.weight(1f))
                }
                // Fill remaining cells with spacers for alignment
                repeat(3 - optionals.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        HorizontalDivider(color = Color(0xFF3A3330))

        // ── k-value + Risk insights ──────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Science, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Text(
                "k ≈ ${node.displayKValue}",
                style = MaterialTheme.typography.labelMedium.copy(
                    color      = tint,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        Text(
            node.displayReinforcement,
            style = MaterialTheme.typography.bodySmall.copy(
                color      = Color(0xFFB0A090),
                lineHeight = 18.sp,
            ),
            maxLines  = 4,
            overflow  = TextOverflow.Ellipsis,
        )

        // ── Timestamp ────────────────────────────────────────────────────────
        Text(
            SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault())
                .format(Date(node.recordedAt)),
            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF5E5550)),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
        )
    }
}

// ── Risk Badge ────────────────────────────────────────────────────────

@Composable
private fun RiskBadge(pClass: RiskLevel, tint: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.15f))
            .border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                pClass.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color      = tint,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
            Text(
                "Risk Level",
                style = MaterialTheme.typography.labelSmall.copy(
                    color    = tint.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                ),
            )
        }
    }
}

// ── Parameter Cell ────────────────────────────────────────────────────────────

@Composable
private fun ParamCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1817))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                color    = Color(0xFF9E8D82),
                fontSize = 10.sp,
            ),
        )
        Text(
            value,
            style = MaterialTheme.typography.labelMedium.copy(
                color      = Color(0xFFEAE1D9),
                fontWeight = FontWeight.SemiBold,
                fontSize   = 11.sp,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Empty / Loading / Error states ───────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = Color(0xFFD97040), modifier = Modifier.size(40.dp), strokeWidth = 3.dp)
            Text("Loading nodes…", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF9E8D82)))
        }
    }
}

@Composable
private fun EmptyContent(onAddNode: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(Icons.Default.Layers, contentDescription = null,
                tint = Color(0xFF4A413B), modifier = Modifier.size(64.dp))
            Text(
                "No Nodes Yet",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFFEAE1D9), fontWeight = FontWeight.Bold),
            )
            Text(
                "Record your first geotechnical bore-log by adding a soil node.",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E8D82)),
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onAddNode,
                shape   = RoundedCornerShape(12.dp),
                colors  = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD97040), contentColor = Color.White),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add First Node", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text("⚠", fontSize = 40.sp)
            Text(message, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFCF6679)),
                textAlign = TextAlign.Center)
        }
    }
}
