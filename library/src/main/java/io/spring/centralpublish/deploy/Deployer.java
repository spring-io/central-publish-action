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

package io.spring.centralpublish.deploy;

import java.nio.file.Path;

import io.spring.centralpublish.bundle.Bundler;
import io.spring.centralpublish.checksum.ChecksumCreator;
import io.spring.centralpublish.file.FileScanner;
import io.spring.centralpublish.log.Logger;
import io.spring.centralpublish.sonatype.CentralPortalApi;
import io.spring.centralpublish.sonatype.PublishingType;
import org.jspecify.annotations.Nullable;

/**
 * Deploys artifacts through the Central Portal API.
 *
 * @author Moritz Halbritter
 */
public interface Deployer {

	Result deploy();

	static Deployer create(Logger logger, Path root, PublishingType publishingType, FileScanner fileScanner,
			ChecksumCreator checksumCreator, Bundler bundler, CentralPortalApi centralPortalApi,
			ArtifactAwaiter artifactAwaiter, boolean dropOnFailure, boolean ignoreAlreadyExistsError,
			@Nullable Coordinates awaitArtifact, @Nullable String name) {
		return new DeployerImpl(logger, root, publishingType, fileScanner, checksumCreator, bundler, centralPortalApi,
				artifactAwaiter, dropOnFailure, ignoreAlreadyExistsError, awaitArtifact, name);
	}

	/**
	 * Deployment result.
	 */
	enum Result {

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
