package com.vrdc.core

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.vrdc.models.HadolintResult
import com.vrdc.util.HadolintPathHelper

object HadolintExecutor {

    private val logger: Logger = Logger.getInstance(HadolintExecutor::class.java)
    private val mapper = ObjectMapper()

    fun runHadolint(content: String, document: Document): List<HadolintResult> {
        return try {
            val hadolintPath = HadolintPathHelper.findHadolintPath()
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

    private fun parseResults(jsonOutput: String, document: Document): List<HadolintResult> {
        return try {
            mapper.readTree(jsonOutput)?.mapNotNull { parseLine(it, document) } ?: emptyList()
        } catch (ex: Exception) {
            if (jsonOutput.isNotBlank()) {
                logger.debug("Failed to parse hadolint output: $jsonOutput")
            }
            emptyList()
        }
    }

    private fun parseLine(json: JsonNode, document: Document): HadolintResult? {
        return try {
            val lineNumber = json["line"].asInt()
            HadolintResult(
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
}
