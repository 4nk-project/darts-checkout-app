package com.example.dartscheckout

import android.content.Context
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode()
        setContent {
            DartsCheckoutApp()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveMode()
    }

    private fun enableImmersiveMode() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }
}

private data class DartThrow(
    val label: String,
    val points: Int,
    val isCheckoutFinish: Boolean,
)

private data class DartHit(
    val dartThrow: DartThrow,
    val highlight: HitHighlight,
)

private data class HitHighlight(
    val sectorIndex: Int? = null,
    val area: HitArea,
)

private enum class HitArea {
    SingleInner,
    SingleOuter,
    Double,
    Triple,
    Bull,
}

private enum class MenuDialog {
    About,
    CheckoutTable,
    Difficulty,
    BoardColors,
    Stats,
    Privacy,
    Licenses,
}

private enum class Difficulty(
    val label: String,
    val range: IntRange,
) {
    Beginner("初級 2-80", 2..80),
    Intermediate("中級 81-120", 81..120),
    Advanced("上級 121-180", 121..180),
    All("全範囲", 2..180),
}

private data class PracticeStats(
    val attempts: Int = 0,
    val correct: Int = 0,
) {
    val accuracy: Int
        get() = if (attempts == 0) 0 else (correct * 100 / attempts)
}

private data class AppUpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val releaseNotesUrl: String?,
    val message: String,
    val forceUpdate: Boolean,
)

private data class BoardColors(
    val lightSegment: Color,
    val ringPrimary: Color,
    val ringSecondary: Color,
)

private data class ColorOption(
    val label: String,
    val color: Color,
)

private enum class ThrowType(
    val shortLabel: String,
    val displayName: String,
    val multiplier: Int,
    val canFinish: Boolean,
) {
    Single("S", "Single", 1, false),
    Double("D", "Double", 2, true),
    Triple("T", "Triple", 3, true),
}

private val boardNumbers = listOf(
    20, 1, 18, 4, 13, 6, 10, 15, 2, 17,
    3, 19, 7, 16, 8, 11, 14, 9, 12, 5,
)

private val defaultBoardColors = BoardColors(
    lightSegment = Color(0xFFF1DDC7),
    ringPrimary = Color(0xFF1D8B4C),
    ringSecondary = Color(0xFFC73B32),
)

private val boardColorOptions = listOf(
    ColorOption("クリーム", Color(0xFFF1DDC7)),
    ColorOption("ホワイト", Color(0xFFF7F2EA)),
    ColorOption("イエロー", Color(0xFFF2C94C)),
    ColorOption("オレンジ", Color(0xFFE47B35)),
    ColorOption("レッド", Color(0xFFC73B32)),
    ColorOption("ピンク", Color(0xFFD95C8A)),
    ColorOption("パープル", Color(0xFF7B61FF)),
    ColorOption("ブルー", Color(0xFF2D7FF9)),
    ColorOption("シアン", Color(0xFF22A6B3)),
    ColorOption("グリーン", Color(0xFF1D8B4C)),
    ColorOption("ライム", Color(0xFF8ABF3F)),
    ColorOption("グレー", Color(0xFF6F665D)),
)

private val allThrows: List<DartThrow> = buildList {
    add(DartThrow("BULL", 50, true))
    for (number in 1..20) {
        add(DartThrow("S$number", number, false))
        add(DartThrow("D$number", number * 2, true))
        add(DartThrow("T$number", number * 3, true))
    }
}

private val finishableScores: List<Int> = (2..180).filter { target ->
    allThrows.any { first ->
        allThrows.any { second ->
            allThrows.any { third ->
                third.isCheckoutFinish && first.points + second.points + third.points == target
            } || second.isCheckoutFinish && first.points + second.points == target
        } || first.isCheckoutFinish && first.points == target
    }
}

private const val contactMailUri = "mailto:contact@ankoromoti.com?subject=Darts%20Checkout%20Feedback"
private const val updateInfoUrl = "https://github.com/4nk-project/darts-checkout-app/releases/latest/download/latest.json"
private const val preferencesName = "darts_checkout_preferences"
private const val lightSegmentColorKey = "board_light_segment_color"
private const val ringPrimaryColorKey = "board_ring_primary_color"
private const val ringSecondaryColorKey = "board_ring_secondary_color"

private val checkoutArrangements = listOf(
    180 to "T20 / T20 / T20",
    170 to "T20 / T20 / BULL",
    167 to "T20 / T19 / BULL",
    164 to "T20 / T18 / BULL",
    161 to "T20 / T17 / BULL",
    160 to "T20 / T20 / D20",
    157 to "T20 / T19 / D20",
    156 to "T20 / T20 / D18",
    154 to "T20 / T18 / D20",
    151 to "T20 / T17 / D20",
    150 to "T20 / T20 / D15",
    140 to "T20 / T20 / D10",
    132 to "BULL / BULL / D16",
    121 to "T20 / T11 / D14",
    100 to "T20 / D20",
    80 to "T20 / D10",
    64 to "T16 / D8",
    50 to "BULL",
    40 to "D20",
    32 to "D16",
)

private fun nextTarget(difficulty: Difficulty): Int {
    val scores = finishableScores.filter { it in difficulty.range }
    return scores.random(Random(System.nanoTime()))
}

private fun recommendedRoute(target: Int): String {
    checkoutArrangements.firstOrNull { it.first == target }?.let { return it.second }

    allThrows.firstOrNull { first ->
        first.isCheckoutFinish && first.points == target
    }?.let { return it.label }

    for (first in allThrows) {
        for (second in allThrows) {
            if (second.isCheckoutFinish && first.points + second.points == target) {
                return "${first.label} / ${second.label}"
            }
        }
    }

    for (first in allThrows) {
        for (second in allThrows) {
            for (third in allThrows) {
                if (third.isCheckoutFinish && first.points + second.points + third.points == target) {
                    return "${first.label} / ${second.label} / ${third.label}"
                }
            }
        }
    }

    return "アレンジが見つかりません"
}

private fun loadBoardColors(context: Context): BoardColors {
    val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    return BoardColors(
        lightSegment = Color(preferences.getInt(lightSegmentColorKey, defaultBoardColors.lightSegment.toArgb())),
        ringPrimary = Color(preferences.getInt(ringPrimaryColorKey, defaultBoardColors.ringPrimary.toArgb())),
        ringSecondary = Color(preferences.getInt(ringSecondaryColorKey, defaultBoardColors.ringSecondary.toArgb())),
    )
}

private fun saveBoardColors(context: Context, boardColors: BoardColors) {
    context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        .edit()
        .putInt(lightSegmentColorKey, boardColors.lightSegment.toArgb())
        .putInt(ringPrimaryColorKey, boardColors.ringPrimary.toArgb())
        .putInt(ringSecondaryColorKey, boardColors.ringSecondary.toArgb())
        .apply()
}

private suspend fun checkForAppUpdate(context: Context): AppUpdateInfo? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL(updateInfoUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
        }
        try {
            if (connection.responseCode !in 200..299) return@runCatching null

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val updateInfo = AppUpdateInfo(
                versionCode = json.getLong("versionCode"),
                versionName = json.optString("versionName", ""),
                apkUrl = json.getString("apkUrl"),
                releaseNotesUrl = json.optString("releaseNotesUrl").takeIf { it.isNotBlank() },
                message = json.optString("message", "新しいバージョンがあります。"),
                forceUpdate = json.optBoolean("forceUpdate", false),
            )

            updateInfo.takeIf { it.versionCode > currentAppVersionCode(context) }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}

private fun currentAppVersionCode(context: Context): Long {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }
}

@Composable
private fun DartsCheckoutApp() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF7F2EA),
        ) {
            CheckoutPracticeScreen()
        }
    }
}

@Composable
private fun CheckoutPracticeScreen() {
    val context = LocalContext.current
    var difficulty by remember { mutableStateOf(Difficulty.All) }
    var target by remember { mutableStateOf(nextTarget(difficulty)) }
    var throws by remember { mutableStateOf(emptyList<DartThrow>()) }
    var result by remember { mutableStateOf<ResultState>(ResultState.Waiting) }
    var menuExpanded by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<MenuDialog?>(null) }
    var answerVisible by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf(PracticeStats()) }
    var boardColors by remember { mutableStateOf(loadBoardColors(context)) }
    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        availableUpdate = checkForAppUpdate(context.applicationContext)
    }

    val total = throws.sumOf { it.points }
    val remaining = target - total
    val enabled = result == ResultState.Waiting && throws.size < 3
    val submitThrow: (DartThrow) -> Unit = { dartThrow ->
        if (result == ResultState.Waiting && throws.size < 3) {
            val nextThrows = throws + dartThrow
            val nextResult = judge(target, nextThrows)
            throws = nextThrows
            result = nextResult
            if (nextResult.isFinal) {
                stats = stats.copy(
                    attempts = stats.attempts + 1,
                    correct = stats.correct + if (nextResult == ResultState.Correct) 1 else 0,
                )
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 30.dp, end = 16.dp, bottom = 12.dp),
    ) {
        val availableHeight = maxHeight

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppTopBar(
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = it },
                onAbout = {
                    menuExpanded = false
                    activeDialog = MenuDialog.About
                },
                onPrivacy = {
                    menuExpanded = false
                    activeDialog = MenuDialog.Privacy
                },
                onLicenses = {
                    menuExpanded = false
                    activeDialog = MenuDialog.Licenses
                },
                onContact = {
                    menuExpanded = false
                    uriHandler.openUri(contactMailUri)
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            QuickActionBar(
                onDifficulty = { activeDialog = MenuDialog.Difficulty },
                onBoardColors = { activeDialog = MenuDialog.BoardColors },
                onCheckoutTable = { activeDialog = MenuDialog.CheckoutTable },
                onStats = { activeDialog = MenuDialog.Stats },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            CheckoutPanel(
                target = target,
                remaining = remaining,
                throws = throws,
                result = result,
                difficulty = difficulty,
                answer = if (answerVisible) recommendedRoute(target) else null,
                onUndo = {
                    throws = throws.dropLast(1)
                    result = ResultState.Waiting
                    answerVisible = false
                },
                onClear = {
                    throws = emptyList()
                    result = ResultState.Waiting
                    answerVisible = false
                },
                onShowAnswer = {
                    answerVisible = true
                },
                onNext = {
                    target = nextTarget(difficulty)
                    throws = emptyList()
                    result = ResultState.Waiting
                    answerVisible = false
                },
                compact = availableHeight < 820.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                val boardSize = if (maxWidth < maxHeight) maxWidth else maxHeight
                DartBoard(
                    modifier = Modifier.size(boardSize),
                    enabled = enabled,
                    boardColors = boardColors,
                    onThrow = submitThrow,
                )
            }
        }
    }

    activeDialog?.let { dialog ->
        AppInfoDialog(
            dialog = dialog,
            difficulty = difficulty,
            boardColors = boardColors,
            stats = stats,
            onDifficultySelected = { selected ->
                difficulty = selected
                target = nextTarget(selected)
                throws = emptyList()
                result = ResultState.Waiting
                answerVisible = false
                activeDialog = null
            },
            onBoardColorsChanged = { colors ->
                boardColors = colors
                saveBoardColors(context, colors)
            },
            onResetStats = {
                stats = PracticeStats()
                activeDialog = null
            },
            onDismiss = { activeDialog = null },
        )
    }

    availableUpdate?.let { updateInfo ->
        AppUpdateDialog(
            updateInfo = updateInfo,
            onUpdate = {
                uriHandler.openUri(updateInfo.apkUrl)
                if (!updateInfo.forceUpdate) {
                    availableUpdate = null
                }
            },
            onReleaseNotes = {
                updateInfo.releaseNotesUrl?.let(uriHandler::openUri)
            },
            onDismiss = {
                if (!updateInfo.forceUpdate) {
                    availableUpdate = null
                }
            },
        )
    }
}

@Composable
private fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    onUpdate: () -> Unit,
    onReleaseNotes: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Text("更新する")
            }
        },
        dismissButton = {
            Row {
                if (updateInfo.releaseNotesUrl != null) {
                    TextButton(onClick = onReleaseNotes) {
                        Text("詳細")
                    }
                }
                if (!updateInfo.forceUpdate) {
                    TextButton(onClick = onDismiss) {
                        Text("あとで")
                    }
                }
            }
        },
        title = {
            Text(
                text = if (updateInfo.versionName.isBlank()) {
                    "アップデートがあります"
                } else {
                    "アップデート ${updateInfo.versionName}"
                },
            )
        },
        text = {
            Text(
                text = updateInfo.message,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color(0xFF18201B),
            )
        },
    )
}

@Composable
private fun AppTopBar(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAbout: () -> Unit,
    onPrivacy: () -> Unit,
    onLicenses: () -> Unit,
    onContact: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Text(
                text = "☰",
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .clickable { onExpandedChange(true) }
                    .padding(top = 3.dp),
                textAlign = TextAlign.Center,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF18201B),
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
            ) {
                DropdownMenuItem(
                    text = { Text("このアプリについて") },
                    onClick = onAbout,
                )
                DropdownMenuItem(
                    text = { Text("プライバシー") },
                    onClick = onPrivacy,
                )
                DropdownMenuItem(
                    text = { Text("ライセンス") },
                    onClick = onLicenses,
                )
                DropdownMenuItem(
                    text = { Text("メールで問い合わせ") },
                    onClick = onContact,
                )
            }
        }
        Text(
            text = "Soft Checkout",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF51483F),
        )
        Spacer(modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun QuickActionBar(
    onDifficulty: () -> Unit,
    onBoardColors: () -> Unit,
    onCheckoutTable: () -> Unit,
    onStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickActionButton(
            text = "難易度",
            modifier = Modifier.weight(1f),
            onClick = onDifficulty,
        )
        QuickActionButton(
            text = "カラー",
            modifier = Modifier.weight(1f),
            onClick = onBoardColors,
        )
        QuickActionButton(
            text = "アレンジ",
            modifier = Modifier.weight(1f),
            onClick = onCheckoutTable,
        )
        QuickActionButton(
            text = "成績",
            modifier = Modifier.weight(1f),
            onClick = onStats,
        )
    }
}

@Composable
private fun QuickActionButton(
    text: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF18201B),
        )
    }
}

@Composable
private fun AppInfoDialog(
    dialog: MenuDialog,
    difficulty: Difficulty,
    boardColors: BoardColors,
    stats: PracticeStats,
    onDifficultySelected: (Difficulty) -> Unit,
    onBoardColorsChanged: (BoardColors) -> Unit,
    onResetStats: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        },
        title = {
            Text(
                text = when (dialog) {
                    MenuDialog.About -> "このアプリについて"
                    MenuDialog.CheckoutTable -> "ダーツのアレンジ表"
                    MenuDialog.Difficulty -> "難易度を選択"
                    MenuDialog.BoardColors -> "盤面カラー設定"
                    MenuDialog.Stats -> "成績"
                    MenuDialog.Privacy -> "プライバシー"
                    MenuDialog.Licenses -> "ライセンス"
                },
            )
        },
        text = {
            when (dialog) {
                MenuDialog.About -> AboutText()
                MenuDialog.CheckoutTable -> CheckoutTable()
                MenuDialog.Difficulty -> DifficultySelector(
                    selected = difficulty,
                    onSelected = onDifficultySelected,
                )
                MenuDialog.BoardColors -> BoardColorSettings(
                    boardColors = boardColors,
                    onBoardColorsChanged = onBoardColorsChanged,
                )
                MenuDialog.Stats -> StatsText(
                    stats = stats,
                    onReset = onResetStats,
                )
                MenuDialog.Privacy -> PrivacyText()
                MenuDialog.Licenses -> LicensesText()
            }
        },
    )
}

@Composable
private fun AboutText() {
    Text(
        text = "表示された残り点を、ソフトダーツのチェックアウトとして3投以内で上がる練習アプリです。盤面をタップすると入力され、合計点と最後のダブル・トリプル・BULL条件で判定します。",
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = Color(0xFF18201B),
    )
}

@Composable
private fun DifficultySelector(
    selected: Difficulty,
    onSelected: (Difficulty) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Difficulty.entries.forEach { difficulty ->
            SecondaryButton(
                text = if (difficulty == selected) "${difficulty.label} 選択中" else difficulty.label,
                modifier = Modifier.fillMaxWidth(),
                enabled = true,
            ) {
                onSelected(difficulty)
            }
        }
    }
}

@Composable
private fun BoardColorSettings(
    boardColors: BoardColors,
    onBoardColorsChanged: (BoardColors) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BoardColorPickerRow(
            label = "シングル白エリア",
            selectedColor = boardColors.lightSegment,
            onColorSelected = { color ->
                onBoardColorsChanged(boardColors.copy(lightSegment = color))
            },
        )
        BoardColorPickerRow(
            label = "リングカラー 1",
            selectedColor = boardColors.ringPrimary,
            onColorSelected = { color ->
                onBoardColorsChanged(boardColors.copy(ringPrimary = color))
            },
        )
        BoardColorPickerRow(
            label = "リングカラー 2",
            selectedColor = boardColors.ringSecondary,
            onColorSelected = { color ->
                onBoardColorsChanged(boardColors.copy(ringSecondary = color))
            },
        )
        SecondaryButton(
            text = "標準カラーに戻す",
            modifier = Modifier.fillMaxWidth(),
            enabled = boardColors != defaultBoardColors,
        ) {
            onBoardColorsChanged(defaultBoardColors)
        }
    }
}

@Composable
private fun BoardColorPickerRow(
    label: String,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ColorSwatch(
                color = selectedColor,
                selected = true,
                size = 34.dp,
                onClick = {},
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF18201B),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            boardColorOptions.chunked(6).forEach { rowColors ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowColors.forEach { option ->
                        ColorSwatch(
                            color = option.color,
                            selected = selectedColor == option.color,
                            contentDescription = option.label,
                            onClick = { onColorSelected(option.color) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    contentDescription: String? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) Color(0xFF18201B) else Color(0xFFE5DED4),
                shape = CircleShape,
            )
            .then(
                if (contentDescription == null) {
                    Modifier
                } else {
                    Modifier.semantics { this.contentDescription = contentDescription }
                },
            )
            .clickable(onClick = onClick),
    )
}

@Composable
private fun StatsText(
    stats: PracticeStats,
    onReset: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "挑戦数 ${stats.attempts}\n正解数 ${stats.correct}\n正答率 ${stats.accuracy}%",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 28.sp,
            color = Color(0xFF18201B),
        )
        SecondaryButton(
            text = "成績をリセット",
            modifier = Modifier.fillMaxWidth(),
            enabled = stats.attempts > 0,
            onClick = onReset,
        )
    }
}

@Composable
private fun PrivacyText() {
    Text(
        text = "このアプリは、個人情報を収集しません。練習中の成績は端末内の画面表示にのみ使用され、外部サーバーへ送信されません。問い合わせメールを送る場合は、メールアプリ側で入力した内容が送信されます。",
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = Color(0xFF18201B),
    )
}

@Composable
private fun LicensesText() {
    Text(
        text = "このアプリはAndroid Jetpack Composeを使用しています。公開時は、リポジトリまたはアプリ説明にOSSライセンス表記を追加してください。",
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = Color(0xFF18201B),
    )
}

@Composable
private fun CheckoutTable() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        checkoutArrangements.forEach { (score, route) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7F2EA), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = score.toString(),
                    modifier = Modifier.width(52.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF18201B),
                )
                Text(
                    text = route,
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF51483F),
                )
            }
        }
    }
}

@Composable
private fun CheckoutPanel(
    target: Int,
    remaining: Int,
    throws: List<DartThrow>,
    result: ResultState,
    difficulty: Difficulty,
    answer: String?,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onShowAnswer: () -> Unit,
    onNext: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val targetSize = if (compact) 46.sp else 66.sp
    val targetLineHeight = if (compact) 50.sp else 70.sp
    val answerHeight = if (compact) 42.dp else 48.dp
    val resultHeight = if (compact) 26.dp else 38.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = target.toString(),
            fontSize = targetSize,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF18201B),
            lineHeight = targetLineHeight,
        )
        Text(
            text = "残り $remaining",
            fontSize = 18.sp,
            color = if (remaining < 0) Color(0xFFB3261E) else Color(0xFF51483F),
        )
        Text(
            text = difficulty.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF6F665D),
        )

        Spacer(modifier = Modifier.height(if (compact) 6.dp else 12.dp))
        AnswerRow(throws = throws, height = answerHeight)
        Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))
        Text(
            text = answer?.let { "答え $it" } ?: result.message,
            modifier = Modifier
                .fillMaxWidth()
                .height(resultHeight),
            textAlign = TextAlign.Center,
            fontSize = if (answer == null) 16.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (answer == null) result.color else Color(0xFF275D38),
            lineHeight = 20.sp,
        )

        Spacer(modifier = Modifier.height(if (compact) 8.dp else 12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryButton(
                text = "戻す",
                modifier = Modifier.weight(1f),
                enabled = throws.isNotEmpty(),
                onClick = onUndo,
            )
            SecondaryButton(
                text = "クリア",
                modifier = Modifier.weight(1f),
                enabled = throws.isNotEmpty(),
                onClick = onClear,
            )
            SecondaryButton(
                text = "答え",
                modifier = Modifier.weight(1f),
                enabled = true,
                onClick = onShowAnswer,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF275D38)),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("次へ")
            }
        }
    }
}

@Composable
private fun AccessibleDartInput(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onThrow: (DartThrow) -> Unit,
) {
    var selectedType by remember { mutableStateOf(ThrowType.Single) }
    var flashLabel by remember { mutableStateOf<String?>(null) }
    val flashAlpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun submit(dartThrow: DartThrow) {
        if (!enabled) return
        flashLabel = dartThrow.label
        scope.launch {
            flashAlpha.snapTo(1f)
            flashAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 420),
            )
        }
        onThrow(dartThrow)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThrowType.entries.forEach { type ->
                TypeButton(
                    type = type,
                    selected = selectedType == type,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) {
                    selectedType = type
                }
            }
        }

        NumberGrid(
            selectedType = selectedType,
            enabled = enabled,
            flashLabel = flashLabel,
            flashAlpha = flashAlpha.value,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onNumber = { number ->
                submit(
                    DartThrow(
                        label = "${selectedType.shortLabel}$number",
                        points = number * selectedType.multiplier,
                        isCheckoutFinish = selectedType.canFinish,
                    ),
                )
            },
        )

        HitButton(
            label = "BULL",
            subLabel = "50",
            enabled = enabled,
            highlighted = flashLabel == "BULL",
            highlightAlpha = flashAlpha.value,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
        ) {
            submit(DartThrow("BULL", 50, true))
        }
    }
}

@Composable
private fun TypeButton(
    type: ThrowType,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val background = when {
        selected -> Color(0xFF275D38)
        enabled -> Color.White
        else -> Color(0xFFE5DED4)
    }
    val contentColor = if (selected) Color.White else Color(0xFF18201B)

    Box(
        modifier = modifier
            .height(54.dp)
            .background(background, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = type.shortLabel,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                lineHeight = 22.sp,
            )
            Text(
                text = type.displayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor.copy(alpha = 0.78f),
                lineHeight = 13.sp,
            )
        }
    }
}

@Composable
private fun NumberGrid(
    selectedType: ThrowType,
    enabled: Boolean,
    flashLabel: String?,
    flashAlpha: Float,
    modifier: Modifier,
    onNumber: (Int) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (1..20).chunked(4).forEach { rowNumbers ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowNumbers.forEach { number ->
                    val label = "${selectedType.shortLabel}$number"
                    HitButton(
                        label = number.toString(),
                        subLabel = "${number * selectedType.multiplier}",
                        enabled = enabled,
                        highlighted = flashLabel == label,
                        highlightAlpha = flashAlpha,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    ) {
                        onNumber(number)
                    }
                }
            }
        }
    }
}

@Composable
private fun HitButton(
    label: String,
    subLabel: String,
    enabled: Boolean,
    highlighted: Boolean,
    highlightAlpha: Float,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val background = when {
        highlighted -> Color(0xFFFFF176).copy(alpha = 0.35f + 0.55f * highlightAlpha)
        enabled -> Color.White
        else -> Color(0xFFE5DED4)
    }
    val textColor = if (enabled) Color(0xFF18201B) else Color(0xFF93897E)

    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                lineHeight = 24.sp,
            )
            Text(
                text = subLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor.copy(alpha = 0.68f),
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun AnswerRow(throws: List<DartThrow>, height: Dp = 48.dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val label = throws.getOrNull(index)?.label ?: "-"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height - 4.dp)
                    .background(Color.White, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF18201B),
                )
            }
            if (index < 2) Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                if (enabled) Color.White else Color(0xFFE5DED4),
                RoundedCornerShape(8.dp),
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color(0xFF18201B) else Color(0xFF93897E),
        )
    }
}

@Composable
private fun DartBoard(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    boardColors: BoardColors,
    onThrow: (DartThrow) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val highlightAlpha = remember { Animatable(0f) }
    var highlight by remember { mutableStateOf<HitHighlight?>(null) }

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (!enabled) return@detectTapGestures
                    hitTestDartBoard(offset, size.width.toFloat(), size.height.toFloat())?.let { hit ->
                        highlight = hit.highlight
                        scope.launch {
                            highlightAlpha.snapTo(1f)
                            highlightAlpha.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 450),
                            )
                        }
                        onThrow(hit.dartThrow)
                    }
                }
            },
    ) {
        drawBoard(
            boardColors = boardColors,
            highlight = highlight,
            highlightAlpha = highlightAlpha.value,
        )
    }
}

private fun DrawScope.drawBoard(
    boardColors: BoardColors,
    highlight: HitHighlight?,
    highlightAlpha: Float,
) {
    val boardSize = minOf(size.width, size.height)
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = boardSize / 2f * 0.96f
    val scoringRadius = radius * 0.84f

    drawCircle(Color(0xFF151515), radius, center)

    for (index in boardNumbers.indices) {
        val start = index * 18f - 99f
        val sweep = 18f
        val dark = index % 2 == 0
        drawRingSector(
            center,
            scoringRadius * 0.12f,
            scoringRadius * 0.49f,
            start,
            sweep,
            if (dark) Color(0xFF1F1E1A) else boardColors.lightSegment,
        )
        drawRingSector(
            center,
            scoringRadius * 0.50f,
            scoringRadius * 0.58f,
            start,
            sweep,
            if (dark) boardColors.ringPrimary else boardColors.ringSecondary,
        )
        drawRingSector(
            center,
            scoringRadius * 0.59f,
            scoringRadius * 0.85f,
            start,
            sweep,
            if (dark) Color(0xFF1F1E1A) else boardColors.lightSegment,
        )
        drawRingSector(
            center,
            scoringRadius * 0.86f,
            scoringRadius * 0.98f,
            start,
            sweep,
            if (dark) boardColors.ringPrimary else boardColors.ringSecondary,
        )
    }

    drawCircle(boardColors.ringPrimary, scoringRadius * 0.11f, center)
    drawCircle(boardColors.ringSecondary, scoringRadius * 0.05f, center)
    drawHitHighlight(center, scoringRadius, highlight, highlightAlpha)
    drawCircle(Color(0xFFEFE8DD), scoringRadius * 0.98f, center, style = Stroke(width = 2.dp.toPx()))
    drawBoardNumbers(center, radius)
}

private fun DrawScope.drawHitHighlight(
    center: Offset,
    scoringRadius: Float,
    highlight: HitHighlight?,
    alpha: Float,
) {
    if (highlight == null || alpha <= 0f) return

    val color = Color(0xFFFFF176).copy(alpha = 0.78f * alpha)
    if (highlight.area == HitArea.Bull) {
        drawCircle(color, scoringRadius * 0.11f, center)
        drawCircle(Color.White.copy(alpha = 0.35f * alpha), scoringRadius * 0.05f, center)
        return
    }

    val sectorIndex = highlight.sectorIndex ?: return
    val start = sectorIndex * 18f - 99f
    val sweep = 18f
    when (highlight.area) {
        HitArea.SingleInner -> drawRingSector(center, scoringRadius * 0.12f, scoringRadius * 0.49f, start, sweep, color)
        HitArea.SingleOuter -> drawRingSector(center, scoringRadius * 0.59f, scoringRadius * 0.85f, start, sweep, color)
        HitArea.Triple -> drawRingSector(center, scoringRadius * 0.50f, scoringRadius * 0.58f, start, sweep, color)
        HitArea.Double -> drawRingSector(center, scoringRadius * 0.86f, scoringRadius * 0.98f, start, sweep, color)
        HitArea.Bull -> Unit
    }
}

private fun DrawScope.drawBoardNumbers(center: Offset, radius: Float) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = radius * 0.08f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val numberRadius = radius * 0.92f
    boardNumbers.forEachIndexed { index, number ->
        val angle = index * 18f * PI.toFloat() / 180f
        val x = center.x + sin(angle) * numberRadius
        val y = center.y - cos(angle) * numberRadius - (paint.descent() + paint.ascent()) / 2f
        drawContext.canvas.nativeCanvas.drawText(number.toString(), x, y, paint)
    }
}

private fun DrawScope.drawRingSector(
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    startAngle: Float,
    sweepAngle: Float,
    color: Color,
) {
    val strokeWidth = outerRadius - innerRadius
    val strokeRadius = innerRadius + strokeWidth / 2f
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - strokeRadius, center.y - strokeRadius),
        size = Size(strokeRadius * 2f, strokeRadius * 2f),
        style = Stroke(width = strokeWidth),
    )
}

private fun hitTestDartBoard(offset: Offset, width: Float, height: Float): DartHit? {
    val boardSize = minOf(width, height)
    val center = Offset(width / 2f, height / 2f)
    val radius = boardSize / 2f * 0.96f * 0.84f
    val dx = offset.x - center.x
    val dy = offset.y - center.y
    val distance = hypot(dx, dy)
    val normalized = distance / radius

    if (normalized > 1.0f) return null
    if (normalized <= 0.11f) {
        return DartHit(
            dartThrow = DartThrow("BULL", 50, true),
            highlight = HitHighlight(area = HitArea.Bull),
        )
    }

    val degrees = ((atan2(dx, -dy) * 180f / PI.toFloat()) + 360f) % 360f
    val sectorIndex = floor((degrees + 9f) / 18f).toInt() % 20
    val number = boardNumbers[sectorIndex]

    return when (normalized) {
        in 0.46f..0.62f -> DartHit(
            dartThrow = DartThrow("T$number", number * 3, true),
            highlight = HitHighlight(sectorIndex, HitArea.Triple),
        )
        in 0.82f..1.0f -> DartHit(
            dartThrow = DartThrow("D$number", number * 2, true),
            highlight = HitHighlight(sectorIndex, HitArea.Double),
        )
        else -> DartHit(
            dartThrow = DartThrow("S$number", number, false),
            highlight = HitHighlight(
                sectorIndex = sectorIndex,
                area = if (normalized < 0.59f) HitArea.SingleInner else HitArea.SingleOuter,
            ),
        )
    }
}

private fun judge(target: Int, throws: List<DartThrow>): ResultState {
    val total = throws.sumOf { it.points }
    val last = throws.lastOrNull()

    return when {
        total > target -> ResultState.Bust
        total == target && last?.isCheckoutFinish == true -> ResultState.Correct
        total == target -> ResultState.NeedsDouble
        throws.size == 3 -> ResultState.Wrong
        else -> ResultState.Waiting
    }
}

private enum class ResultState(
    val message: String,
    val color: Color,
    val isFinal: Boolean,
) {
    Waiting("", Color.Transparent, false),
    Correct("正解", Color(0xFF1B6B3A), true),
    Bust("バースト", Color(0xFFB3261E), true),
    NeedsDouble("最後はダブル・トリプル・BULLで上がってください", Color(0xFFB26A00), true),
    Wrong("不正解", Color(0xFFB3261E), true),
}
