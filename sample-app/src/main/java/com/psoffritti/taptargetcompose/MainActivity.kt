package com.psoffritti.taptargetcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.psoffritti.taptargetcompose.ui.theme.AppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      AppTheme {
        TapTargetCoordinator(showTapTargets = true, onComplete = {}) {
          Surface(modifier = Modifier.fillMaxSize()) {
            Content()
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TapTargetScope.Content() {
  val toolbarTapTarget = getStandardTapTargetDefinition(
    precedence = 1,
    title = "Toolbar tap target",
    description = "This is a toolbar, tap it!",
  )

  val tab2TapTarget = getStandardTapTargetDefinition(
    precedence = 2,
    title = "Tab2 tap target",
    description = "A moving target",
  )

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.surface,
    topBar = {
      TopAppBar(title = {
        Text(text = "Tap Target", modifier = Modifier.tapTarget(toolbarTapTarget))
      })
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = {  },
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text(text = "Click here") },
        modifier = Modifier.tapTarget(
          getStandardTapTargetDefinition(
            precedence = 0,
            title = "Button tap target",
            description = "This is a button, tap it!",
          )
        ),
      )
    },
    floatingActionButtonPosition = FabPosition.End,
  ) { innerPadding ->
    Text("Content", modifier = Modifier.padding(innerPadding))
    TabbedControls(onTabSelected = {}) { tab ->
      when (tab) {
        Tab.TAB1 -> Text("Tab 1 content")
        Tab.TAB2 -> Text("Tab 2 content", modifier = Modifier.tapTarget(tab2TapTarget))
      }
    }
  }
}

private enum class Tab {
  TAB1, TAB2;
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabbedControls(
  onTabSelected: (Tab) -> Unit,
  modifier: Modifier = Modifier,
  contentProvider: @Composable (Tab) -> Unit
) {
  val scope = rememberCoroutineScope()

  val pagerState = rememberPagerState(pageCount = { Tab.entries.size })
  val tabTitles = Tab.entries.map {
    when (it) {
      Tab.TAB1 -> "Tab1"
      Tab.TAB2 -> "Tan2"
    }
  }

  Column(modifier = modifier) {
    TabRow(selectedTabIndex = pagerState.currentPage) {
      tabTitles.forEachIndexed { index, title ->
        Tab(
          selected = index == pagerState.currentPage,
          onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
          text = { Text(title) }
        )
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalPager(state = pagerState, modifier = Modifier.height(128.dp)) { pageIndex ->
      contentProvider(Tab.entries[pageIndex])
    }

    LaunchedEffect(pagerState.settledPage) {
      // Trigger the callback each time settledPage changes.
      onTabSelected(Tab.entries[pagerState.settledPage])
    }
  }
}

@Composable
private fun getStandardTapTargetDefinition(
  precedence: Int,
  title: String,
  description: String,
  onTargetCLick: () -> Unit = {},
  onTargetCancel: () -> Unit = {},
): TapTargetDefinition {
  return TapTargetDefinition(
    precedence = precedence,
    title = TextDefinition(
      text = title,
      textStyle = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSecondaryContainer
    ),
    description = TextDefinition(
      text = description,
      textStyle = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSecondaryContainer
    ),
    tapTargetStyle = TapTargetStyle(
      backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
      tapTargetHighlightColor = MaterialTheme.colorScheme.onSecondaryContainer,
      backgroundAlpha = 1f,
    ),
    onTargetClick = onTargetCLick,
    onTargetCancel = onTargetCancel,
  )
}