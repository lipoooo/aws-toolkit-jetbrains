// Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.core.tools

import java.nio.file.Path

/**
 * Represents an executable external program such as a CLI
 *
 * Note: It is recommended that all implementations of this interface are stateless and are an `object`
 */
interface ToolType<VersionScheme : Version> {
    /**
     * ID used to represent the executable in caches and settings. Must be globally unique
     */
    val id: String

    /**
     * Name of the executable for users, e.g. the marketing name of the executable
     */
    val displayName: String

    /**
     * List of supported [VersionRange]. An empty list means any version is supported
     */
    fun supportedVersions(): List<VersionRange<VersionScheme>> = emptyList()

    /**
     * Returns the [Version] for the executable of this type located at the specified location
     */
    fun determineVersion(path: Path): VersionScheme
}

/**
 * Indicates that a [ToolType] can be auto-discovered for the user
 */
interface AutoResolvable {
    /**
     * Attempt to automatically resolve the path
     *
     * @return the resolved path or null if not found
     * @throws Exception if an exception occurred attempting to resolve the path
     */
    fun resolve(): Path?
}