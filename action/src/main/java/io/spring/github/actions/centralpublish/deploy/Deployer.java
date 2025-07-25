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

package io.spring.github.actions.centralpublish.deploy;

import java.nio.file.Path;

import io.spring.github.actions.centralpublish.bundle.Bundle;
import io.spring.github.actions.centralpublish.bundle.Bundler;
import io.spring.github.actions.centralpublish.checksum.ChecksumCreator;
import io.spring.github.actions.centralpublish.file.FileScanner;
import io.spring.github.actions.centralpublish.file.FileSet;
import io.spring.github.actions.centralpublish.sonatype.CentralPortalApi;
import io.spring.github.actions.centralpublish.sonatype.Deployment;
import io.spring.github.actions.centralpublish.sonatype.PublishingType;
import io.spring.github.actions.centralpublish.system.Logger;

import org.springframework.lang.Nullable;

/**
 * Deployer for deploying to the Sontype Central Portal.
 *
 * @author Moritz Halbritter
 */
public class Deployer {

	private final Logger logger;

	private final FileScanner fileScanner;

	private final ChecksumCreator checksumCreator;

	private final Bundler bundleCreator;

	private final CentralPortalApi centralPortalApi;

	private final ArtifactAwaiter artifactAwaiter;

	private final Path root;

	private final PublishingType publishingType;

	private final boolean dropDeploymentOnFailure;

	private final @Nullable Coordinates awaitArtifact;

	Deployer(Logger logger, Path root, PublishingType publishingType, FileScanner fileScanner,
			ChecksumCreator checksumCreator, Bundler bundler, CentralPortalApi centralPortalApi,
			ArtifactAwaiter artifactAwaiter, boolean dropDeploymentOnFailure, @Nullable Coordinates awaitArtifact) {
		this.logger = logger;
		this.root = root;
		this.publishingType = publishingType;
		this.fileScanner = fileScanner;
		this.checksumCreator = checksumCreator;
		this.bundleCreator = bundler;
		this.centralPortalApi = centralPortalApi;
		this.artifactAwaiter = artifactAwaiter;
		this.dropDeploymentOnFailure = dropDeploymentOnFailure;
		this.awaitArtifact = awaitArtifact;
	}

	public void validate() {
		if (this.awaitArtifact != null && this.publishingType != PublishingType.AUTOMATIC) {
			throw new IllegalStateException("Await artifact can only be used if publishing type is automatic");
		}
	}

	public Result deploy() {
		FileSet files = this.fileScanner.scan(this.root);
		if (files.isEmpty()) {
			throw new IllegalStateException("No files found in directory '%s'".formatted(this.root));
		}
		this.logger.log("Found {} files, creating checksums ...", files.size());
		FileSet checksums = this.checksumCreator.createChecksums(files);
		files = files.plus(checksums);
		this.logger.log("Checksums created. Creating bundle with {} files ...", files.size());
		Bundle bundle = this.bundleCreator.createBundle(this.root, files);
		this.logger.log("Bundle created. Uploading {} to Sonatype ...", bundle.getSize());
		Deployment deployment = this.centralPortalApi.upload(bundle, this.publishingType);
		this.logger.log("Bundle uploaded, resulting in deployment '{}'.", deployment.getId());
		this.logger.log("Awaiting final status ...");
		deployment.awaitFinalStatus();
		return switch (deployment.getStatus()) {
			case FAILED -> deploymentFailed(deployment);
			case PUBLISHED -> deploymentPublished(deployment);
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
		return Result.SUCCESS;
	}

	private Result deploymentPublished(Deployment deployment) {
		if (this.publishingType == PublishingType.USER_MANAGED) {
			throw new IllegalStateException(
					"Publishing type USER_MANAGED should only have states FAILED or VALIDATED, but got PUBLISHED");
		}
		this.logger.log("Deployment '{}' successfully published", deployment.getId());
		if (this.awaitArtifact != null) {
			this.logger.log("Waiting for artifact to appear");
			this.artifactAwaiter.await(this.awaitArtifact);
		}
		return Result.SUCCESS;
	}

	private Result deploymentFailed(Deployment deployment) {
		this.logger.error("Deployment '{}' failed: {}", deployment.getId(), deployment.getErrors());
		if (this.dropDeploymentOnFailure) {
			this.logger.log("Dropping deployment");
			deployment.drop();
		}
		return Result.FAILURE;
	}

	/**
	 * Deployment result.
	 */
	public enum Result {

		/**
		 * Success.
		 */
		SUCCESS,
		/**
		 * Failure.
		 */
		FAILURE

	}

}
