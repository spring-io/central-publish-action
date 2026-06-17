/*
 * Copyright 2025 - present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.maven.central.deploy;

import java.nio.file.Path;

import io.spring.maven.central.bundle.Bundle;
import io.spring.maven.central.bundle.Bundler;
import io.spring.maven.central.checksum.ChecksumCreator;
import io.spring.maven.central.file.FileScanner;
import io.spring.maven.central.file.FileSet;
import io.spring.maven.central.log.Logger;
import io.spring.maven.central.sonatype.CentralPortalApi;
import io.spring.maven.central.sonatype.Deployment;
import io.spring.maven.central.sonatype.Errors;
import io.spring.maven.central.sonatype.PublishingType;
import org.jspecify.annotations.Nullable;

/**
 * Deployer for deploying to the Sontype Central Portal.
 *
 * @author Moritz Halbritter
 */
class DeployerImpl implements Deployer {

	private final Logger logger;

	private final FileScanner fileScanner;

	private final ChecksumCreator checksumCreator;

	private final Bundler bundleCreator;

	private final CentralPortalApi centralPortalApi;

	private final ArtifactAwaiter artifactAwaiter;

	private final PublishingType publishingType;

	DeployerImpl(Logger logger, PublishingType publishingType, FileScanner fileScanner, ChecksumCreator checksumCreator,
			Bundler bundler, CentralPortalApi centralPortalApi, ArtifactAwaiter artifactAwaiter) {
		this.logger = logger;
		this.publishingType = publishingType;
		this.fileScanner = fileScanner;
		this.checksumCreator = checksumCreator;
		this.bundleCreator = bundler;
		this.centralPortalApi = centralPortalApi;
		this.artifactAwaiter = artifactAwaiter;
	}

	@Override
	public Result deploy(Path root, boolean dropDeploymentOnFailure, boolean ignoreAlreadyExistsError,
			@Nullable Coordinates awaitArtifact, @Nullable String deploymentName) {
		if (awaitArtifact != null && this.publishingType != PublishingType.AUTOMATIC) {
			throw new IllegalStateException("Await artifact can only be used if publishing type is automatic");
		}
		FileSet files = this.fileScanner.scan(root);
		if (files.isEmpty()) {
			throw new IllegalStateException("No files found in directory '%s'".formatted(root));
		}
		this.logger.log("Found {} files, creating checksums ...", files.size());
		FileSet checksums = this.checksumCreator.createChecksums(files);
		if (checksums.isEmpty()) {
			this.logger.log("No checksums created. Creating bundle with {} files ...", files.size());
		}
		else {
			files = files.plus(checksums);
			this.logger.log("Checksums created. Creating bundle with {} files ...", files.size());
		}
		Deployment deployment;
		try (Bundle bundle = this.bundleCreator.createBundle(root, files)) {
			this.logger.log("Bundle created. Uploading {} to Sonatype ...", bundle.getSize());
			deployment = this.centralPortalApi.upload(bundle, this.publishingType, deploymentName);
		}
		this.logger.log("Bundle uploaded, resulting in deployment '{}'.", deployment.getId());
		this.logger.log("Awaiting final status ...");
		deployment.awaitFinalStatus();
		return switch (deployment.getStatus()) {
			case FAILED -> deploymentFailed(deployment, ignoreAlreadyExistsError, dropDeploymentOnFailure);
			case PUBLISHED -> deploymentPublished(deployment, awaitArtifact);
			case VALIDATED -> deploymentValidated(deployment);
			default -> throw new IllegalStateException(
					"Unexpected deployment status value %s".formatted(deployment.getStatus()));
		};
	}

	private Result deploymentValidated(Deployment deployment) {
		if (this.publishingType == PublishingType.AUTOMATIC) {
			throw new IllegalStateException(
					"Publishing type AUTOMATIC should only have states FAILED or PUBLISHED, but got VALIDATED");
		}
		this.logger.log("Deployment '{}' is done. Please execute required manual steps to publish the deployment.",
				deployment.getId());
		return Result.success(deployment);
	}

	private Result deploymentPublished(Deployment deployment, @Nullable Coordinates awaitArtifact) {
		if (this.publishingType == PublishingType.USER_MANAGED) {
			throw new IllegalStateException(
					"Publishing type USER_MANAGED should only have states FAILED or VALIDATED, but got PUBLISHED");
		}
		this.logger.log("Deployment '{}' successfully published", deployment.getId());
		if (awaitArtifact != null) {
			this.logger.log("Waiting for artifact to appear");
			this.artifactAwaiter.await(awaitArtifact);
		}
		return Result.success(deployment);
	}

	private Result deploymentFailed(Deployment deployment, boolean ignoreAlreadyExistsError,
			boolean dropDeploymentOnFailure) {
		Errors errors = deployment.getErrors();
		if (ignoreAlreadyExistsError && errors.hasOnlyAlreadyExistsError()) {
			this.logger.log("Deployment '{}' has already been deployed", deployment.getId());
			dropDeployment(deployment, dropDeploymentOnFailure);
			return Result.success(deployment);
		}
		this.logger.error("Deployment '{}' failed", deployment.getId());
		this.logger.error("Errors:\n\n{}", errors);
		dropDeployment(deployment, dropDeploymentOnFailure);
		return Result.failure(deployment);
	}

	private void dropDeployment(Deployment deployment, boolean dropDeploymentOnFailure) {
		if (dropDeploymentOnFailure) {
			this.logger.log("Dropping deployment");
			deployment.drop();
		}
	}

}
