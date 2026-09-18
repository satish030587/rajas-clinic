package com.rajashomoeocare.clinic.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajashomoeocare.clinic.R
import com.rajashomoeocare.clinic.ui.theme.BrandBlue
import com.rajashomoeocare.clinic.ui.theme.BrandRed

/**
 * The logo lockup: vector mark plus live text. Text rather than baked-in artwork so
 * the wordmark stays crisp at any density and follows the user's font scale.
 */
@Composable
fun BrandLockup(
    modifier: Modifier = Modifier,
    markSize: Int = 64,
    showTagline: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.logo_mark),
            contentDescription = null,
            modifier = Modifier.size(markSize.dp),
        )
        Column {
            Text(
                text = "Rajas",
                fontFamily = FontFamily.Cursive,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                fontSize = (markSize * 0.42f).sp,
                color = BrandBlue,
            )
            Text(
                text = "Homoeo Care",
                fontWeight = FontWeight.ExtraBold,
                fontSize = (markSize * 0.27f).sp,
                letterSpacing = 0.5.sp,
                color = BrandRed,
            )
            if (showTagline) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandBlue,
                )
            }
        }
    }
}
