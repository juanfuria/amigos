package com.efetepe.amigos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp

@Composable
fun AboutViewContent() {
    val colorScheme = MaterialTheme.colorScheme

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("About", style = MaterialTheme.typography.titleLarge)

            Text("Amigos v1.0.0", style = MaterialTheme.typography.titleMedium)

            Text(
                "A small utility to help the dispersed minded stay in touch with friends and loved ones.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            val annotatedString = buildAnnotatedString {
                append("Icons by ")
                withLink(
                    LinkAnnotation.Url(
                        url = "https://icons8.com/",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    )
                ) {
                    append("Icons8")
                }
            }

            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
