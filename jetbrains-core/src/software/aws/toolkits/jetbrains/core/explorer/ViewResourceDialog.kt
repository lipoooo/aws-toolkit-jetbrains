// Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.core.explorer

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import javax.swing.JComponent

class ViewResourceDialog(project: Project, val resourceType: String, actionTitle: String, checkResourceNameValidity: (resource: String?) -> Boolean) :
    DialogWrapper(project) {
    var resourceName = ""
    private val component by lazy {
        panel {
            row("$resourceType:") {
                textField(::resourceName).constraints(grow).withErrorOnApplyIf("$resourceType must be entered") {
                    it.text.isNullOrBlank() || checkResourceNameValidity(it.text)
                }
            }
        }
    }

    init {
        title = actionTitle
        init()
    }

    override fun createCenterPanel(): JComponent = component
}
