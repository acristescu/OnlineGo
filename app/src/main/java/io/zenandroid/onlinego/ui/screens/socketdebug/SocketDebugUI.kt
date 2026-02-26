package io.zenandroid.onlinego.ui.screens.socketdebug

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zenandroid.onlinego.data.repositories.SocketEvent
import io.zenandroid.onlinego.data.repositories.SocketEventType
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocketDebugScreen(
  viewModel: SocketDebugViewModel = koinViewModel(),
  onNavigateBack: () -> Unit,
) {
  val events by viewModel.events.collectAsStateWithLifecycle()
  val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
  val filter by viewModel.filter.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

  val filteredEvents by remember(events, filter, searchQuery) {
    derivedStateOf {
      events.asReversed().filter { event ->
        (filter == null || event.type == filter) &&
            (searchQuery.isBlank() ||
                event.tag.contains(searchQuery, ignoreCase = true) ||
                event.message.contains(searchQuery, ignoreCase = true))
      }
    }
  }

  val listState = rememberLazyListState()

  LaunchedEffect(filteredEvents.size) {
    if (filteredEvents.isNotEmpty() && listState.firstVisibleItemIndex <= 1) {
      listState.scrollToItem(0)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Socket Debug")
            Spacer(modifier = Modifier.width(12.dp))
            ConnectionBadge(connectionState)
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { viewModel.clear() }) {
            Icon(Icons.Default.Delete, contentDescription = "Clear log")
          }
        }
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      // Search bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { viewModel.setSearchQuery(it) },
        placeholder = { Text("Filter events...", fontSize = 13.sp) },
        singleLine = true,
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { viewModel.setSearchQuery("") }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear")
            }
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 4.dp),
        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
      )

      // Filter chips
      Row(
        modifier = Modifier
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = filter == null,
          onClick = { viewModel.setFilter(null) },
          label = { Text("All (${events.size})", fontSize = 12.sp) },
        )
        SocketEventType.entries.forEach { type ->
          val count = events.count { it.type == type }
          FilterChip(
            selected = filter == type,
            onClick = { viewModel.setFilter(if (filter == type) null else type) },
            label = { Text("${type.name} ($count)", fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = type.chipColor().copy(alpha = 0.2f),
            ),
          )
        }
      }

      // Event count
      Text(
        text = "${filteredEvents.size} events",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
      )

      // Event list
      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        items(filteredEvents) { event ->
          EventRow(event)
        }
      }
    }
  }
}

@Composable
private fun ConnectionBadge(state: String) {
  val color = when (state) {
    "Connected" -> Color(0xFF4CAF50)
    "Connecting..." -> Color(0xFFFFC107)
    else -> Color(0xFFF44336)
  }
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(color)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = state,
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun EventRow(event: SocketEvent) {
  val bgColor = when (event.type) {
    SocketEventType.SENT -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    SocketEventType.RECEIVED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
    SocketEventType.STATE -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
    SocketEventType.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(6.dp))
      .background(bgColor)
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth(),
    ) {
      TypeBadge(event.type)
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = event.formattedTime,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = event.tag,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
      )
    }
    if (event.message.isNotBlank()) {
      Text(
        text = event.message,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 14.sp,
        maxLines = 6,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
      )
    }
  }
}

@Composable
private fun TypeBadge(type: SocketEventType) {
  val (text, color) = when (type) {
    SocketEventType.SENT -> "→" to Color(0xFF2196F3)
    SocketEventType.RECEIVED -> "←" to Color(0xFF4CAF50)
    SocketEventType.STATE -> "●" to Color(0xFFFFC107)
    SocketEventType.ERROR -> "✗" to Color(0xFFF44336)
  }
  Text(
    text = text,
    fontSize = 14.sp,
    fontWeight = FontWeight.Bold,
    color = color,
  )
}

private fun SocketEventType.chipColor(): Color = when (this) {
  SocketEventType.SENT -> Color(0xFF2196F3)
  SocketEventType.RECEIVED -> Color(0xFF4CAF50)
  SocketEventType.STATE -> Color(0xFFFFC107)
  SocketEventType.ERROR -> Color(0xFFF44336)
}




