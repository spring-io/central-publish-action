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

import io.spring.github.actions.centralpublish.CentralPublishProperties;
import io.spring.github.actions.centralpublish.bundle.Bundler;
import io.spring.github.actions.centralpublish.checksum.ChecksumCreator;
import io.spring.github.actions.centralpublish.file.FileScanner;
import io.spring.github.actions.centralpublish.sonatype.CentralPortalApi;
import io.spring.github.actions.centralpublish.sonatype.PublishingType;
import io.spring.github.actions.centralpublish.system.Logger;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Configuration for deployment beans.
 *
 * @author Moritz Halbritter
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CentralPublishProperties.class)
class DeployerConfiguration {

	@Bean
	Deployer deployer(CentralPublishProperties properties, Logger logger, FileScanner fileScanner,
			ChecksumCreator checksumCreator, Bundler bundler, CentralPortalApi centralPortalApi,
			ArtifactAwaiter artifactAwaiter) {
		CentralPublishProperties.Deployment deployment = properties.getDeployment();
		return new Deployer(logger, properties.getDirectoryAsPath(), getPublishingType(deployment), fileScanner,
				checksumCreator, bundler, centralPortalApi, artifactAwaiter, deployment.isDropOnFailure(),
				deployment.isIgnoreAlreadyExistsError(), getAwaitArtifact(deployment), deployment.getName());
	}

	@Bean
	ArtifactAwaiter artifactAwaiter(CentralPublishProperties properties, Logger logger,
			RestClient.Builder restClientBuilder) {
		return new ArtifactAwaiter(logger, properties.getDeployment().getTimeout(),
				properties.getDeployment().getSleepBetweenRetries(), properties.getMavenCentralBaseUri(),
				restClientBuilder);
	}

	private @Nullable Coordinates getAwaitArtifact(CentralPublishProperties.Deployment properties) {
		String awaitArtifact = properties.getAwaitArtifact();
		if (!StringUtils.hasLength(awaitArtifact)) {
			return null;
		}
		return Coordinates.parse(awaitArtifact);
	}

	private PublishingType getPublishingType(CentralPublishProperties.Deployment properties) {
		return switch (properties.getPublishingType()) {
			case AUTOMATIC -> PublishingType.AUTOMATIC;
			case USER_MANAGED -> PublishingType.USER_MANAGED;
		};
	}

}
