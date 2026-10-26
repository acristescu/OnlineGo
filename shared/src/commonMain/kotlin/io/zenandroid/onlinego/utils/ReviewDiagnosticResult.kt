package io.zenandroid.onlinego.utils

data class ReviewDiagnosticResult(
  val success: Boolean,
  val steps: List<String>,
  val error: Throwable?
) {
  fun toDisplayString(): String = buildString {
    appendLine(if (success) "=== REVIEW FLOW SUCCEEDED ===" else "=== REVIEW FLOW FAILED ===")
    appendLine()
    steps.forEachIndexed { index, step ->
      appendLine("${index + 1}. $step")
    }
    if (error != null) {
      appendLine()
      appendLine("Error type: ${error::class.qualifiedName}")
      appendLine("Error message: ${error.message}")
    }
  }
}
