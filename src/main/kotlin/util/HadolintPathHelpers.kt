package com.vrdc.util

import com.intellij.openapi.diagnostic.Logger
import java.io.File

object HadolintPathHelper {
    private val logger: Logger = Logger.getInstance(HadolintPathHelper::class.java)

    private fun getHadolintPaths(): List<String> {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("mac") -> listOf(
                "/usr/local/bin/hadolint", "/opt/homebrew/bin/hadolint"
            )

            os.contains("nix") || os.contains("nux") || os.contains("aix") -> listOf(
                "/usr/local/bin/hadolint", "/usr/bin/hadolint", "/bin/hadolint"
            )

            os.contains("win") -> listOf(
                System.getenv("ProgramFiles") + "\\Hadolint\\hadolint.exe",
                System.getenv("LocalAppData") + "\\Hadolint\\hadolint.exe",
                "C:\\tools\\hadolint\\hadolint.exe"
            )

            else -> emptyList()
        }
    }

    fun findHadolintPath(): String? {
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
