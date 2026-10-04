package com.kyant.backdrop.catalog.destinations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    RoundedRect("Round"),
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
) {
    companion object {
        val Default = ShapeSettings()
    }
}

@Composable
fun GlassPlaygroundContent() {
    val animationScope = rememberCoroutineScope()
    val offsetAnimation = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val zoomAnimation = remember { Animatable(1f) }
    val rotationAnimation = remember { Animatable(0f) }

    var isSheetExpanded by remember { mutableStateOf(true) }
    var lastTapMillis by remember { mutableStateOf(0L) }

    var shapeType by remember { mutableStateOf(ShapeType.RoundedRect) }
    val settingsMap = remember { mutableStateMapOf<ShapeType, ShapeSettings>() }
    val currentSettings = settingsMap[shapeType] ?: ShapeSettings.Default

    fun updateSettings(transform: (ShapeSettings) -> ShapeSettings) {
        val current = settingsMap[shapeType] ?: ShapeSettings.Default
        settingsMap[shapeType] = transform(current)
    }

    fun resetCurrentShape() {
        settingsMap.remove(shapeType)
        settingsMap[shapeType] = ShapeSettings.Default
        animationScope.launch {
            launch { offsetAnimation.animateTo(Offset.Zero) }
            launch { zoomAnimation.animateTo(1f) }
            launch { rotationAnimation.animateTo(0f) }
        }
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
                        val c = cos(angleInRadians)
                        val s = sin(angleInRadians)
                        return Offset((x * c - y * s).toFloat(), (x * s + y * c).toFloat())
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
                        .navigationBarsPadding()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(28f.dp) },
                            effects = {
                                vibrancy()
                                blur(4f.dp.toPx())
                                lens(16f.dp.toPx(), 32f.dp.toPx())
                            },
                            highlight = { Highlight.Plain },
                            exportedBackdrop = sheetBackdrop,
                            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) }
                        )
                        .padding(14f.dp)
                        .align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6f.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4f.dp)
                    ) {
                        ShapeType.values().forEach { type ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .background(
                                        if (shapeType == type) Color(0xFFFF8D28)
                                        else Color(0x55000000)
                                    )
                                    .clickable { shapeType = type }
                                    .padding(vertical = 8f.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(type.label, style = TextStyle(Color.White, 11f.sp))
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8f.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            MiniControl("Pos X", currentSettings.posX,
                                { v -> updateSettings { it.copy(posX = v) } },
                                -500f..500f, 5f, 0.1f, sheetBackdrop)
                        }
                        Box(Modifier.weight(1f)) {
                            MiniControl("Pos Y", currentSettings.posY,
                                { v -> updateSettings { it.copy(posY = v) } },
                                -500f..500f, 5f, 0.1f, sheetBackdrop)
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8f.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            MiniControl("Width", currentSettings.widthDp,
                                { v -> updateSettings { it.copy(widthDp = v) } },
                                32f..600f, 4f, 0.1f, sheetBackdrop)
                        }
                        Box(Modifier.weight(1f)) {
                            MiniControl("Height", currentSettings.heightDp,
                                { v -> updateSettings { it.copy(heightDp = v) } },
                                32f..600f, 4f, 0.1f, sheetBackdrop)
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8f.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            MiniControl("Corner", currentSettings.cornerRadiusFrac,
                                { v -> updateSettings { it.copy(cornerRadiusFrac = v) } },
                                0f..1f, 0.05f, 0.001f, sheetBackdrop)
                        }
                        Box(Modifier.weight(1f)) {
                            MiniControl("Blur", currentSettings.blurRadiusDp,
                                { v -> updateSettings { it.copy(blurRadiusDp = v) } },
                                0f..32f, 1f, 0.01f, sheetBackdrop)
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8f.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            MiniControl("RefHeight", currentSettings.refractionHeightFrac,
                                { v -> updateSettings { it.copy(refractionHeightFrac = v) } },
                                0f..1f, 0.05f, 0.001f, sheetBackdrop)
                        }
                        Box(Modifier.weight(1f)) {
                            MiniControl("RefAmt", currentSettings.refractionAmountFrac,
                                { v -> updateSettings { it.copy(refractionAmountFrac = v) } },
                                0f..1f, 0.05f, 0.001f, sheetBackdrop)
                        }
                    }

                    MiniControl("Chromatic", currentSettings.chromaticAberration,
                        { v -> updateSettings { it.copy(chromaticAberration = v) } },
                        0f..1f, 0.05f, 0.001f, sheetBackdrop)

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4f.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            FlatBtn("Hide", Color(0xFF555555)) { isSheetExpanded = false }
                        }
                        Box(Modifier.weight(1f)) {
                            FlatBtn("Image", Color(0xFF2196F3)) { /* pick image */ }
                        }
                        Box(Modifier.weight(1f)) {
                            FlatBtn("Reset", Color(0xFFFF8D28)) { resetCurrentShape() }
                        }
                        Box(Modifier.weight(1f)) {
                            FlatBtn("🔼", Color(0xFFFF8D28)) { isSheetExpanded = false }
                        }
                    }
                }
            } else {
                Box(
                    Modifier
                        .padding(20f.dp)
                        .navigationBarsPadding()
                        .align(Alignment.BottomCenter)
                ) {
                    FlatBtn("🔼", Color(0xFFFF8D28)) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapMillis < 600L) {
                            isSheetExpanded = true
                            lastTapMillis = 0L
                        } else {
                            lastTapMillis = now
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlatBtn(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(color)
            .clickable { onClick() }
            .padding(vertical = 10f.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(label, style = TextStyle(Color.White, 12f.sp))
    }
}

@Composable
private fun MiniControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    visibilityThreshold: Float,
    backdrop: Backdrop
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2f.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicText(label, style = TextStyle(fontSize = 10f.sp))
            BasicText(formatValue(value), style = TextStyle(fontSize = 10f.sp))
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4f.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(24f.dp)
                    .background(Color(0x66000000))
                    .clickable { onValueChange((value - step).coerceIn(valueRange)) },
                contentAlignment = Alignment.Center
            ) {
                BasicText("−", style = TextStyle(Color.White, 14f.sp))
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
            Box(
                Modifier
                    .size(24f.dp)
                    .background(Color(0x66000000))
                    .clickable { onValueChange((value + step).coerceIn(valueRange)) },
                contentAlignment = Alignment.Center
            ) {
                BasicText("+", style = TextStyle(Color.White, 14f.sp))
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
