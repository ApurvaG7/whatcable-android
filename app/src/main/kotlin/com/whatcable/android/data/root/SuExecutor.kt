package com.whatcable.android.data.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuExecutor @Inject constructor() {

    data class Result(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    ) {
        val success: Boolean get() = exitCode == 0
    }

    suspend fun execute(vararg args: String, timeoutMs: Long = 5000): Result? {
        if (args.isEmpty()) return null
        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(timeoutMs) {
                try {
                    val command = listOf("su", "-c") + args.toList()
                    val process = ProcessBuilder(command)
                        .redirectErrorStream(false)
                        .start()

                    val stdout = BufferedReader(InputStreamReader(process.inputStream))
                        .use { it.readText() }
                    val stderr = BufferedReader(InputStreamReader(process.errorStream))
                        .use { it.readText() }
                    val exitCode = process.waitFor()

                    Result(exitCode, stdout.trim(), stderr.trim())
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    suspend fun readFile(path: String): String? {
        if (path.isBlank()) return null
        val result = execute("cat", shellEscape(path), timeoutMs = 5000) ?: return null
        return if (result.success) result.stdout else null
    }

    suspend fun listDirectory(path: String): List<String> {
        if (path.isBlank()) return emptyList()
        val result = execute("ls", shellEscape(path), timeoutMs = 5000) ?: return emptyList()
        if (!result.success) return emptyList()
        return result.stdout.lines().filter { it.isNotBlank() }
    }

    suspend fun isAvailable(): Boolean {
        val result = execute("id", timeoutMs = 3000) ?: return false
        return result.success && result.stdout.contains("uid=0")
    }

    private fun shellEscape(input: String): String {
        return "'" + input.replace("'", "'\\''") + "'"
    }
}
