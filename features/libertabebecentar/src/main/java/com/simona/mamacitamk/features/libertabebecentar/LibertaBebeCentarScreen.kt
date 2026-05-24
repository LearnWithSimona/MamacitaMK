package com.simona.mamacitamk.features.libertabebecentar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simona.mamacitamk.core.ui_state.RenderContent

@Composable
fun LibertaBebeCentarScreen(
    modifier: Modifier = Modifier,
    viewModel: LibertaBebeCentarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        state.RenderContent(
            idleContent = { CircularProgressIndicator() },
            loadingContent = { CircularProgressIndicator() },
            emptyDataContent = { Text("No products") },
            onErrorContent = { error -> Text("Error: ${error.message ?: "Unknown"}") },
            successContent = { products ->
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                ) {
                    items(products) { p ->
                        Column {
                            Text(text = p.name)
                            val price = p.effectivePrice?.let { "$it ${p.priceCurrency.orEmpty()}" } ?: "—"
                            Text(text = price)
                        }
                    }
                }
            },
        )
    }
}
