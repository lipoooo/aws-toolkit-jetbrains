// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cloudwatch.logs

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.util.text.DateFormatUtil
import icons.AwsIcons
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient
import software.aws.toolkits.core.utils.error
import software.aws.toolkits.core.utils.getLogger
import software.aws.toolkits.jetbrains.core.applicationThreadPoolScope
import software.aws.toolkits.jetbrains.core.toolwindow.ToolkitToolWindowManager
import software.aws.toolkits.jetbrains.core.toolwindow.ToolkitToolWindowType
import software.aws.toolkits.jetbrains.services.cloudwatch.logs.editor.CloudWatchLogGroup
import software.aws.toolkits.jetbrains.services.cloudwatch.logs.editor.CloudWatchLogStream
import software.aws.toolkits.jetbrains.services.cloudwatch.logs.insights.DetailedLogRecord
import software.aws.toolkits.jetbrains.services.cloudwatch.logs.insights.QueryDetails
import software.aws.toolkits.jetbrains.services.cloudwatch.logs.insights.QueryResultList
import software.aws.toolkits.jetbrains.utils.getCoroutineUiContext
import software.aws.toolkits.resources.message
import software.aws.toolkits.telemetry.CloudwatchlogsTelemetry
import software.aws.toolkits.telemetry.Result
import java.time.Duration

class CloudWatchLogWindow(private val project: Project) {
    private val coroutineScope = applicationThreadPoolScope(project)
    private val toolWindow = ToolkitToolWindowManager.getInstance(project, CW_LOGS_TOOL_WINDOW)
    private val edtContext = getCoroutineUiContext()

    suspend fun showLogGroup(logGroup: String) {
        var result = Result.Succeeded
        try {
            if (showWindow(logGroup)) {
                return
            }
            val group = CloudWatchLogGroup(project, logGroup)
            val title = message("cloudwatch.logs.log_group_title", logGroup.split("/").last())
            withContext(edtContext) {
                toolWindow.addTab(title, group.content, activate = true, id = logGroup, disposable = group, refresh = { group.refreshTable() })
            }
        } catch (e: Exception) {
            LOG.error(e) { "Exception thrown while trying to show log group '$logGroup'" }
            result = Result.Failed
            throw e
        } finally {
            CloudwatchlogsTelemetry.openGroup(project, result)
        }
    }

    suspend fun showLogStream(
        logGroup: String,
        logStream: String,
        previousEvent: LogStreamEntry? = null,
        duration: Duration? = null,
        streamLogs: Boolean = false
    ) {
        var result = Result.Succeeded
        try {
            val id = "$logGroup/$logStream/${previousEvent?.timestamp}/${previousEvent?.message}/$duration"
            if (showWindow(id)) {
                return
            }
            val title = if (previousEvent != null && duration != null) {
                message(
                    "cloudwatch.logs.filtered_log_stream_title",
                    logStream,
                    DateFormatUtil.getDateTimeFormat().format(previousEvent.timestamp - duration.toMillis()),
                    DateFormatUtil.getDateTimeFormat().format(previousEvent.timestamp + duration.toMillis())
                )
            } else {
                message("cloudwatch.logs.log_stream_title", logStream)
            }
            val stream = CloudWatchLogStream(project, logGroup, logStream, previousEvent, duration, streamLogs)
            withContext(edtContext) {
                toolWindow.addTab(title, stream.content, activate = true, id = id, disposable = stream, refresh = { stream.refreshTable() })
            }
        } catch (e: Exception) {
            LOG.error(e) { "Exception thrown while trying to show log group '$logGroup' stream '$logStream'" }
            result = Result.Failed
            throw e
        } finally {
            CloudwatchlogsTelemetry.openStream(project, result)
        }
    }

    suspend fun showQueryResults(queryDetails: QueryDetails, queryId: String, fields: List<String>) {
        if (showWindow(queryId)) {
            return
        }

        val queryResult = QueryResultList(project, fields, queryId, queryDetails)
        val title = message("cloudwatch.logs.query_tab_title", queryId)
        withContext(edtContext) {
            toolWindow.addTab(title, queryResult.resultsPanel, activate = true, id = queryId, disposable = queryResult)
        }
    }

    suspend fun showDetailedEvent(client: CloudWatchLogsClient, identifier: String) {
        if (showWindow(identifier)) {
            return
        }

        val detailedLogEvent = DetailedLogRecord(project, client, identifier)
        withContext(edtContext) {
            toolWindow.addTab(detailedLogEvent.title, detailedLogEvent.getComponent(), activate = true, id = identifier, disposable = detailedLogEvent)
        }
    }

    suspend fun closeLogGroup(logGroup: String) {
        toolWindow.findPrefix(logGroup).forEach {
            withContext(edtContext) {
                it.dispose()
            }
        }
    }

    private suspend fun showWindow(identifier: String): Boolean {
        val existingWindow = toolWindow.find(identifier)
        if (existingWindow != null) {
            withContext(edtContext) {
                existingWindow.show(refresh = true)
            }
            return true
        }
        return false
    }

    companion object {
        internal val CW_LOGS_TOOL_WINDOW = ToolkitToolWindowType(
            "AWS.CloudWatchLog",
            message("cloudwatch.logs.toolwindow"),
            AwsIcons.Resources.CloudWatch.LOGS_TOOL_WINDOW
        )

        fun getInstance(project: Project) = ServiceManager.getService(project, CloudWatchLogWindow::class.java)
        private val LOG = getLogger<CloudWatchLogWindow>()
    }
}
