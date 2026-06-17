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

import io.spring.maven.central.bundle.Bundler;
import io.spring.maven.central.checksum.ChecksumCreator;
import io.spring.maven.central.file.FileScanner;
import io.spring.maven.central.log.Logger;
import io.spring.maven.central.sonatype.CentralPortalApi;
import io.spring.maven.central.sonatype.Deployment;
import io.spring.maven.central.sonatype.PublishingType;
import org.jspecify.annotations.Nullable;

/**
 * Deploys artifacts through the Central Portal API.
 *
 * @author Moritz Halbritter
 */
public interface Deployer {

	/**
	 * Deploys the artifacts to the Central Portal.
	 * @param root the root directory of the artifacts
	 * @return the deployment result
	 */
	Result deploy(Path root);

	/**
	 * Creates a new {@link Deployer}.
	 * @param logger the logger
	 * @param publishingType the publishing type
	 * @param fileScanner the file scanner
	 * @param checksumCreator the checksum creator
	 * @param bundler the bundler
	 * @param centralPortalApi the Central Portal API client
	 * @param artifactAwaiter the artifact awaiter
	 * @param dropOnFailure whether to drop the deployment on failure
	 * @param ignoreAlreadyExistsError whether to ignore "already exists" errors
	 * @param awaitArtifact the coordinates of the artifact to await, or {@code null}
	 * @param name the deployment name, or {@code null}
	 * @return the {@link Deployer}
	 */
	static Deployer create(Logger logger, PublishingType publishingType, FileScanner fileScanner,
			ChecksumCreator checksumCreator, Bundler bundler, CentralPortalApi centralPortalApi,
			ArtifactAwaiter artifactAwaiter, boolean dropOnFailure, boolean ignoreAlreadyExistsError,
			@Nullable Coordinates awaitArtifact, @Nullable String name) {
		return new DeployerImpl(logger, publishingType, fileScanner, checksumCreator, bundler, centralPortalApi,
				artifactAwaiter, dropOnFailure, ignoreAlreadyExistsError, awaitArtifact, name);
	}

	/**
	 * Deployment result.
	 *
	 * @param status the status
	 * @param deployment the deployment
	 */
	record Result(Status status, Deployment deployment) {

		/**
		 * Creates a successful deployment result.
		 * @param deployment the deployment
		 * @return the successful deployment result
		 */
		public static Result success(Deployment deployment) {
			return new Result(Status.SUCCESS, deployment);
		}

		/**
		 * Creates a failed deployment result.
		 * @param deployment the deployment
		 * @return the failed deployment result
		 */
		public static Result failure(Deployment deployment) {
			return new Result(Status.FAILURE, deployment);
		}

		/**
		 * Deployment result status.
		 */
		public enum Status {

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

}
