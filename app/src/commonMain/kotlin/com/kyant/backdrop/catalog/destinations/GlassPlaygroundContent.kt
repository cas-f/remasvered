package com.kyant.backdrop.catalog.destinations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.catalog.Block
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.catalog.components.LiquidSlider
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class ShapeType(val label: String) {
    RoundedRect("Rounded"),
    Circle("Circle"),
    Square("Square"),
    Pill("Pill")
}

data class ShapeSettings(
    val posX: Float = 0f,
    val posY: Float = 0f,
    val widthDp: Float = 256f,
    val heightDp: Float = 256f,
    val cornerRadiusFrac: Float = 0.5f,
    val blurRadiusDp: Float = 0f,
    val refractionHeightFrac: Float = 0.2f,
    val refractionAmountFrac: Float = 0.2f,
    val chromaticAberration: Float = 0f
)

@Composable
fun GlassPlaygroundContent() {
    val animationScope = rememberCoroutineScope()
    val offsetAnimation = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val zoomAnimation = remember { Animatable(1f) }
    val rotationAnimation = remember { Animatable(0f) }

    var isSheetExpanded by remember { mutableStateOf(true) }

    var shapeType by remember { mutableStateOf(ShapeType.RoundedRect) }
    val settingsMap = remember { mutableStateMapOf<ShapeType, ShapeSettings>() }
    val currentSettings = settingsMap[shapeType] ?: ShapeSettings()

    fun updateSettings(transform: (ShapeSettings) -> ShapeSettings) {
        val current = settingsMap[shapeType] ?: ShapeSettings()
        settingsMap[shapeType] = transform(current)
    }

    BackdropDemoScaffold { backdrop ->
        Box(
            Modifier
                .padding(top = 48f.dp)
                .statusBarsPadding()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = {
                        when (shapeType) {
                            ShapeType.RoundedRect -> RoundedRectangle(
                                currentSettings.widthDp.dp / 2f * currentSettings.cornerRadiusFrac
                            )
                            ShapeType.Circle -> RoundedRectangle(
                                minOf(currentSettings.widthDp, currentSettings.heightDp).dp / 2f
                            )
                            ShapeType.Square -> RoundedRectangle(0f.dp)
                            ShapeType.Pill -> RoundedRectangle(currentSettings.heightDp.dp / 2f)
                        }
                    },
                    effects = {
                        val minDimension = size.minDimension
                        vibrancy()
                        blur(currentSettings.blurRadiusDp.dp.toPx())
                        lens(
                            refractionHeight = currentSettings.refractionHeightFrac * minDimension * 0.5f,
                            refractionAmount = currentSettings.refractionAmountFrac * minDimension,
                            depthEffect = true,
                            chromaticAberration = currentSettings.chromaticAberration > 0f
                        )
                    },
                    highlight = { Highlight.Plain },
                    layerBlock = {
                        val offset = offsetAnimation.value
                        val zoom = zoomAnimation.value
                        val rotation = rotationAnimation.value
                        translationX = offset.x + currentSettings.posX
                        translationY = offset.y + currentSettings.posY
                        scaleX = zoom
                        scaleY = zoom
                        rotationZ = rotation
                    }
                )
                .pointerInput(animationScope) {
                    fun Offset.rotateBy(angle: Float): Offset {
                        val angleInRadians = angle * (PI / 180)
                        val cos = cos(angleInRadians)
                        val sin = sin(angleInRadians)
                        return Offset((x * cos - y * sin).toFloat(), (x * sin + y * cos).toFloat())
                    }

                    detectTransformGestures { _, pan, gestureZoom, gestureRotate ->
                        val offset = offsetAnimation.value
                        val zoom = zoomAnimation.value
                        val rotation = rotationAnimation.value

                        val targetZoom = zoom * gestureZoom
                        val targetRotation = rotation + gestureRotate
                        val targetOffset = offset + pan.rotateBy(targetRotation) * targetZoom

                        animationScope.launch {
                            offsetAnimation.snapTo(targetOffset)
                            zoomAnimation.snapTo(targetZoom)
                            rotationAnimation.snapTo(targetRotation)
                        }
                    }
                }
                .size(currentSettings.widthDp.dp, currentSettings.heightDp.dp)
                .align(Alignment.TopCenter)
        )

        Block {
            if (isSheetExpanded) {
                val sheetBackdrop = rememberLayerBackdrop()
                Column(
                    Modifier
                        .padding(16f.dp)
                        .padding(bottom = 72f.dp)
                        .navigationBarsPadding()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(32f.dp) },
                            effects = {
                                vibrancy()
                                blur(4f.dp.toPx())
                                lens(16f.dp.toPx(), 32f.dp.toPx())
                            },
                            highlight = { Highlight.Plain },
                            exportedBackdrop = sheetBackdrop,
                            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) }
                        )
                        .padding(24f.dp)
                        .align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12f.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6f.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShapeType.values().forEach { type ->
                            Box(Modifier.weight(1f)) {
                                LiquidButton(
                                    { shapeType = type },
                                    sheetBackdrop,
                                    Modifier.fillMaxWidth(),
                                    tint = if (shapeType == type) Color(0xFFFF8D28) else Color.Gray
                                ) {
                                    BasicText(
                                        type.label,
                                        style = TextStyle(Color.White, 12f.sp)
                                    )
                                }
                            }
                        }
                    }

                    ControlRow(
                        label = "Pos X",
                        value = currentSettings.posX,
                        onValueChange = { v -> updateSettings { it.copy(posX = v) } },
                        valueRange = -500f..500f,
                        step = 5f,
                        visibilityThreshold = 0.1f,
                        backdrop = sheetBackdrop
                    )
                    ControlRow(
                        label = "Pos Y",
                        value = currentSettings.posY,
                        onValueChange = { v -> updateSettings { it.copy(posY = v) } },
                        valueRange = -500f..500f,
                        step = 5f,
                        visibilityThreshold = 0.1f,
                        backdrop = sheetBackdrop
                    )
                    ControlRow(
                        label = "Width",
                        value = currentSettings.widthDp,
                        onValueChange = { v -> updateSettings { it.copy(widthDp = v) } },
                        valueRange = 32f..600f,
                        step = 4f,
                        visibilityThreshold = 0.1f,
                        backdrop = sheetBackdrop
                    )
                    ControlRow(
                        label = "Height",
                        value = currentSettings.heightDp,
                        onValueChange = { v -> updateSettings { it.copy(heightDp = v) } },
                        valueRange = 32f..600f,
                        step = 4f,
                        visibilityThreshold = 0.1f,
                        backdrop = sheetBackdrop
                    )
                    ControlRow(
                        label = "Corner radius",
                        value = currentSettings.cornerRadiusFrac,
                        onValueChange = { v -> updateSettings { it.copy(cornerRadiusFrac = v) } },
                        valueRange = 0f..1f,
                        step = 0.05f,
                        visibilityThreshold = 0.001f,
                        backdrop = sheetBackdrop
                    )
                    ControlRow(
                        label = "Blur radius",
                        value = currentSettings.blurRadiusDp,
                        onValueChange = { v -> updateSettings { it.copy(blurRadiusDp = v) } },
                        valueRange = 0f..32f,
                        step = 1f,
                        visibilityThreshold = 0.01f,
                        backdrop = sheetBackdrop
                    )
                    ControlRow(
                        label = "Refraction height",
                        value = currentSettings.refractionHeightFrac,
                        onValueChange = { v -> updateSettings { it.copy(refractionHeightFrac = v) } },
                        valueRange = 0f..1f,
                        step = 0.05f,
                        visibilityThreshold = 0.001f,
                        backdrop = sheetBackdrop
                    )
                    ControlRow(
                        label = "Refraction amount",
                        value = currentSettings.refractionAmountFrac,
                        onValueChange = { v -> updateSettings { it.copy(refractionAmountFrac = v) } },
                        valueRange = 0f..1f,
                        step = 0.05f,
                        visibilityThreshold = 0.001f,
                        backdrop = sheetBackdrop
                    )
                    ControlRow(
                        label = "Chromatic aberration",
                        value = currentSettings.chromaticAberration,
                        onValueChange = { v -> updateSettings { it.copy(chromaticAberration = v) } },
                        valueRange = 0f..1f,
                        step = 0.05f,
                        visibilityThreshold = 0.001f,
                        backdrop = sheetBackdrop
                    )
                }
            }
        }

        Block {
            LiquidButton(
                { isSheetExpanded = !isSheetExpanded },
                backdrop,
                Modifier
                    .padding(20f.dp)
                    .navigationBarsPadding()
                    .align(Alignment.BottomStart),
                tint = Color(0xFFFF8D28)
            ) {
                BasicText(
                    if (isSheetExpanded) "🔽" else "🔼",
                    style = TextStyle(Color.White, 15f.sp)
                )
            }

            LiquidButton(
                {
                    animationScope.launch {
                        launch { offsetAnimation.animateTo(Offset.Zero) }
                        launch { zoomAnimation.animateTo(1f) }
                        launch { rotationAnimation.animateTo(0f) }
                    }
                    settingsMap[shapeType] = ShapeSettings()
                },
                backdrop,
                Modifier
                    .padding(20f.dp)
                    .navigationBarsPadding()
                    .align(Alignment.BottomEnd),
                tint = Color(0xFFFF8D28)
            ) {
                BasicText(
                    "Reset",
                    style = TextStyle(Color.White, 15f.sp)
                )
            }
        }
    }
}

@Composable
private fun ControlRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    visibilityThreshold: Float,
    backdrop: Backdrop
) {
    Column(verticalArrangement = Arrangement.spacedBy(6f.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(label, style = TextStyle(fontSize = 13f.sp))
            BasicText(formatValue(value), style = TextStyle(fontSize = 13f.sp))
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8f.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidButton(
                { onValueChange((value - step).coerceIn(valueRange)) },
                backdrop,
                Modifier.size(36f.dp),
                tint = Color(0xFFFF8D28)
            ) {
                BasicText("−", style = TextStyle(Color.White, 18f.sp))
            }
            Box(Modifier.weight(1f)) {
                LiquidSlider(
                    value = { value },
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    visibilityThreshold = visibilityThreshold,
                    backdrop = backdrop
                )
            }
            LiquidButton(
                { onValueChange((value + step).coerceIn(valueRange)) },
                backdrop,
                Modifier.size(36f.dp),
                tint = Color(0xFFFF8D28)
            ) {
                BasicText("+", style = TextStyle(Color.White, 18f.sp))
            }
        }
    }
}

private fun formatValue(v: Float): String {
    return if (abs(v - v.roundToInt()) < 0.01f) {
        v.roundToInt().toString()
    } else {
        ((v * 100).roundToInt() / 100f).toString()
    }
}
