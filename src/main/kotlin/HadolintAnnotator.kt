package com.vrdc

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.vrdc.models.HadolintResult
import com.vrdc.core.HadolintExecutor

class HadolintAnnotator : ExternalAnnotator<PsiFile, List<HadolintResult>>() {

    override fun collectInformation(file: PsiFile): PsiFile? {
        return if (file.name.equals("Dockerfile", ignoreCase = true)) file else null
    }

    override fun doAnnotate(file: PsiFile?): List<HadolintResult> {
        if (file == null || !file.isValid) return emptyList()
        val document: Document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return emptyList()
        return HadolintExecutor.runHadolint(document.text, document)
    }

    override fun apply(file: PsiFile, results: List<HadolintResult>, holder: AnnotationHolder) {
        results.forEach { result ->
            result.range?.takeIf { range ->
                range.endOffset <= file.textLength && range.startOffset >= 0
            }?.let { safeRange ->
                val severity = when (result.severity.lowercase()) {
                    "error" -> HighlightSeverity.ERROR
                    "warning" -> HighlightSeverity.WARNING
                    else -> HighlightSeverity.WEAK_WARNING
                }
                holder.newAnnotation(severity, result.message).range(safeRange).create()
            }
        }
    }
}
