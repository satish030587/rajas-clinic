package com.rajashomoeocare.clinic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rajashomoeocare.clinic.R
import com.rajashomoeocare.clinic.data.local.PatientRow
import com.rajashomoeocare.clinic.data.local.Sex
import com.rajashomoeocare.clinic.domain.age
import com.rajashomoeocare.clinic.domain.shortDate
import com.rajashomoeocare.clinic.util.formatPhone

@Composable
fun PatientCard(
    patient: PatientRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    statusText: String? = null,
    statusContainer: Color? = null,
    statusContent: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PatientAvatar(name = patient.name)
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patient.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    patient.sex.icon()?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    Text(
                        text = buildString {
                            patient.age?.let { append("$it yrs · ") }
                            append(formatPhone(patient.phone))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (statusText != null && statusContainer != null && statusContent != null) {
                    Spacer(Modifier.height(8.dp))
                    StatusPill(
                        text = statusText,
                        container = statusContainer,
                        content = statusContent,
                        icon = Icons.Outlined.CalendarMonth,
                    )
                } else if (patient.lastVisitDate != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.common_last_seen,
                            patient.lastVisitDate.shortDate(),
                        ) + " · " + pluralStringResource(
                            R.plurals.visit_count,
                            patient.visitCount,
                            patient.visitCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

private fun Sex.icon(): ImageVector? = when (this) {
    Sex.MALE -> Icons.Filled.Male
    Sex.FEMALE -> Icons.Filled.Female
    Sex.OTHER -> null
}
