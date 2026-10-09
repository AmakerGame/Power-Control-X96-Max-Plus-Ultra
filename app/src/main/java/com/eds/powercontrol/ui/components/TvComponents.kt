package com.eds.powercontrol.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TV D-pad interactive Card with smooth scale animation and glowing focus border.
 */
@Composable
fun TvFocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    testTag: String = "tv_card",
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        animationSpec = tween(150),
        label = "tv_card_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color(0xFF00E5FF)
            isSelected -> Color(0xFF00B0FF)
            else -> Color(0x334A5568)
        },
        animationSpec = tween(150),
        label = "tv_card_border"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color(0xFF242C38)
            isSelected -> Color(0xFF1E2836)
            else -> Color(0xFF161B22)
        },
        animationSpec = tween(150),
        label = "tv_card_bg"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(
                border = BorderStroke(if (isFocused) 3.dp else if (isSelected) 2.dp else 1.dp, borderColor),
                shape = RoundedCornerShape(14.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .testTag(testTag)
    ) {
        content(isFocused)
    }
}

/**
 * TV Button optimized for remote D-Pad navigation.
 */
@Composable
fun TvFocusableButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    testTag: String = "tv_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = tween(150),
        label = "tv_button_scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused && isPrimary -> Color(0xFF00E5FF)
            isFocused && !isPrimary -> Color(0xFF3B485A)
            isPrimary -> Color(0xFF0091EA)
            else -> Color(0xFF263238)
        },
        animationSpec = tween(150),
        label = "tv_button_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused && isPrimary -> Color(0xFF05111D)
            else -> Color.White
        },
        animationSpec = tween(150),
        label = "tv_button_content_color"
    )

    val borderStroke = if (isFocused) {
        BorderStroke(2.5.dp, Color.White)
    } else {
        BorderStroke(1.dp, Color(0x44FFFFFF))
    }

    Surface(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = borderStroke
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 16.sp
            )
        }
    }
}
