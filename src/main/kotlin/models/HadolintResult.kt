package com.vrdc.models

import com.intellij.openapi.util.TextRange

data class HadolintResult(
    val line: Int, val message: String, val severity: String, val range: TextRange?
)
