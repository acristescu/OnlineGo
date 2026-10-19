package io.zenandroid.onlinego.ui.composables

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
class ExposedLazyDropdownMenuTest {

  @Test
  fun anExpandedMenuComposesOnlyTheVisibleRows() = runComposeUiTest {
    var composedRows = 0
    setContent {
      ExposedDropdownMenuBox(expanded = true, onExpandedChange = {}) {
        Text("anchor", Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable))
        ExposedLazyDropdownMenu(
          expanded = true,
          items = (0 until 1_000).toList(),
          onDismissRequest = {},
        ) { _, item ->
          composedRows++
          Text("Row $item")
        }
      }
    }

    onNodeWithText("Row 0").assertIsDisplayed()
    onNodeWithText("Row 999").assertDoesNotExist()
    check(composedRows in 1..<100) { "composed $composedRows rows" }
  }
}
