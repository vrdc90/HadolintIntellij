package com.vrdc

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

class HadolintAnnotator : ExternalAnnotator<PsiFile, List<HadolintAnnotator.Result>>() {

    private val logger: Logger = Logger.getInstance(HadolintAnnotator::class.java)
    private val mapper = ObjectMapper()

    data class Result(
        val line: Int, val message: String, val severity: String, val range: TextRange?
    )

    override fun collectInformation(file: PsiFile): PsiFile? {
        return if (file.name.equals("Dockerfile", ignoreCase = true)) file else null
    }

    override fun doAnnotate(file: PsiFile?): List<Result> {
        if (file == null || !file.isValid) return emptyList()

        val document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return emptyList()

        return runHadolint(document.text, document)
    }

    private fun runHadolint(content: String, document: Document): List<Result> {
        return try {
            val process = ProcessBuilder("hadolint", "--format", "json", "-").apply {
                redirectErrorStream(true)
            }.start()

            process.outputStream.use { it.write(content.toByteArray()) }
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode !in listOf(0, 1)) {
                logger.warn("Hadolint error (code $exitCode): $output")
            }

            parseResults(output, document)
        } catch (ex: Exception) {
            logger.error("Hadolint execution failed: ${ex.message}")
            emptyList()
        }
    }

    private fun parseResults(jsonOutput: String, document: Document): List<Result> {
        return try {
            mapper.readTree(jsonOutput)?.mapNotNull { parseLine(it, document) } ?: emptyList()
        } catch (ex: Exception) {
            if (jsonOutput.isNotBlank()) {
                logger.debug("Failed to parse hadolint output: $jsonOutput")
            }
            emptyList()
        }
    }

    private fun parseLine(json: JsonNode, document: Document): Result? {
        return try {
            val lineNumber = json["line"].asInt()

            Result(
                line = lineNumber,
                message = json["message"].asText(),
                severity = json["level"].asText(),
                range = calculateFullLineRange(document, lineNumber)
            )
        } catch (ex: Exception) {
            logger.debug("Skipping invalid issue: ${json.toPrettyString()}")
            null
        }
    }

    private fun calculateFullLineRange(document: Document, line: Int): TextRange? {
        return try {
            val lineIndex = line - 1
            val startOffset = document.getLineStartOffset(lineIndex)
            val endOffset = document.getLineEndOffset(lineIndex)
            TextRange(startOffset, endOffset).takeIf { it.length > 0 }
        } catch (ex: Exception) {
            logger.debug("Invalid line $line in document")
            null
        }
    }

    override fun apply(file: PsiFile, results: List<Result>, holder: AnnotationHolder) {
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
