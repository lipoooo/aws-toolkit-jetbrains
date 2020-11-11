// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.core.utils

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest
import java.util.UUID

fun createIntegrationTestCredentialProvider(): AwsCredentialsProvider {
    val defaultCredentials = DefaultCredentialsProvider.create()

    return System.getenv("ASSUME_ROLE_ARN")?.takeIf { it.isNotEmpty() }?.let { role ->
        val sessionName = UUID.randomUUID().toString()

        StsAssumeRoleCredentialsProvider.builder()
            .stsClient(StsClient.builder().credentialsProvider(defaultCredentials).build())
            .refreshRequest(AssumeRoleRequest.builder().roleArn(role).roleSessionName(sessionName).build())
            .build()
    } ?: defaultCredentials
}
