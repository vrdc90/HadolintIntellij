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
import java.io.File

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
            val hadolintPath = findHadolintPath()
                ?: throw IllegalStateException("hadolint not found in common paths or system PATH")

            val process = ProcessBuilder(hadolintPath, "--format", "json", "-").apply {
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

    private fun getHadolintPaths(): List<String> {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("mac") -> listOf(
                "/usr/local/bin/hadolint",
                "/opt/homebrew/bin/hadolint"
            )

            os.contains("nix") || os.contains("nux") || os.contains("aix") -> listOf(
                "/usr/local/bin/hadolint",
                "/usr/bin/hadolint",
                "/bin/hadolint"
            )

            os.contains("win") -> listOf(
                System.getenv("ProgramFiles") + "\\Hadolint\\hadolint.exe",
                System.getenv("LocalAppData") + "\\Hadolint\\hadolint.exe",
                "C:\\tools\\hadolint\\hadolint.exe"
            )

            else -> emptyList()
        }
    }

    private fun findHadolintPath(): String? {
        getHadolintPaths().forEach { path ->
            if (File(path).exists()) {
                logger.info("Found hadolint at: $path")
                return path
            }
        }

        return try {
            val command = if (System.getProperty("os.name").lowercase().contains("win")) {
                arrayOf("where", "hadolint")
            } else {
                arrayOf("which", "hadolint")
            }

            val process = ProcessBuilder(*command).start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            if (process.exitValue() == 0 && output.isNotEmpty()) {
                output.lineSequence().firstOrNull()?.trim()
            } else {
                null
            }
        } catch (ex: Exception) {
            logger.error("Error searching PATH for hadolint: ${ex.message}")
            null
        }
    }
}
