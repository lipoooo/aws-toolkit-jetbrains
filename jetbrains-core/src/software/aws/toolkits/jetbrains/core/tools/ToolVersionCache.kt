// Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.core.tools

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.util.ThrowableComputable
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * Stores data related to a file path. Cache is invalidated when the cache entry is detected as stale.  Errors are
 * cached until the underlying path is detected as stale. Stale is defined as the cache entries (file modification time)[Files.getLastModifiedTime]
 * is older than the path's current modification time.
 */
class ToolVersionCache {
    private val cache = ConcurrentHashMap<Tool<*>, Result<*>>()
    private val lock = ReentrantLock()

    @Suppress("UNCHECKED_CAST")
    fun <V : Version> getValue(tool: Tool<ToolType<V>>): Result<V> = ProgressIndicatorUtils.computeWithLockAndCheckingCanceled(
        lock, 50, TimeUnit.MILLISECONDS,
        ThrowableComputable {
            val lastResult = cache[tool]
            var lastModifiedTime = 0L
            try {
                lastModifiedTime = getLastModificationTime(tool.path)
                if (lastResult == null || lastResult.lastModifiedTime < lastModifiedTime) {
                    Result.Success(testExecutable(tool), lastModifiedTime).also {
                        cache[tool] = it
                    } as Result<V>
                } else {
                    lastResult as Result<V>
                }
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (e: Exception) {
                Result.Failure(e, lastModifiedTime).also {
                    cache[tool] = it
                } as Result<V>
            }
        }
    )

    private fun <T : Version> testExecutable(tool: Tool<ToolType<T>>): Version = tool.type.determineVersion(tool.path)

    private fun getLastModificationTime(path: Path): Long = Files.getLastModifiedTime(path).toMillis()

    sealed class Result<T : Version>(open val lastModifiedTime: Long) {
        data class Failure(val reason: Exception, override val lastModifiedTime: Long) : Result<Nothing>(lastModifiedTime)
        data class Success<V : Version>(val version: V, override val lastModifiedTime: Long) : Result<V>(lastModifiedTime)
    }
}