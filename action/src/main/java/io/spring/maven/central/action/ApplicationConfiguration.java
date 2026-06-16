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

package io.spring.maven.central.action;

import java.time.Clock;

import io.spring.maven.central.bundle.Bundler;
import io.spring.maven.central.checksum.ChecksumCreator;
import io.spring.maven.central.checksum.ChecksumPolicy;
import io.spring.maven.central.deploy.ArtifactAwaiter;
import io.spring.maven.central.deploy.Coordinates;
import io.spring.maven.central.deploy.Deployer;
import io.spring.maven.central.file.FileScanner;
import io.spring.maven.central.log.Logger;
import io.spring.maven.central.sonatype.CentralPortalApi;
import io.spring.maven.central.sonatype.PublishingType;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Spring application configuration.
 *
 * @author Moritz Halbritter
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CentralPublishProperties.class)
class ApplicationConfiguration {

	private final CentralPublishProperties properties;

	ApplicationConfiguration(CentralPublishProperties properties) {
		this.properties = properties;
	}

	@Bean
	FileScanner fileScanner() {
		return FileScanner.create();
	}

	@Bean
	ChecksumCreator creator(Logger logger) {
		return ChecksumCreator.create(logger, (this.properties.getChecksum().isFailOnExistingChecksums()
				? ChecksumPolicy.FAIL_ON_EXISTING : ChecksumPolicy.OVERWRITE_EXISTING));
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	Logger logger() {
		if (runsOnGithubActions()) {
			boolean debugEnabled = Boolean.parseBoolean(System.getenv("ACTIONS_STEP_DEBUG"));
			return Logger.githubActions(debugEnabled);
		}
		return Logger.slf4j();
	}

	@Bean
	RestClientCustomizer restClientCustomizer() {
		return (builder) -> builder.defaultHeader("User-Agent", "central-publish-action");
	}

	@Bean
	Bundler bundler() {
		return Bundler.create();
	}

	@Bean
	CentralPortalApi centralPortalApi(CentralPublishProperties properties, Logger logger,
			RestClient.Builder restClientBuilder, Clock clock) {
		CentralPublishProperties.Token token = properties.getToken();
		CentralPublishProperties.Deployment deployment = properties.getDeployment();
		return CentralPortalApi.create(logger, properties.getBaseUri(), token.getName(), token.getValue(),
				restClientBuilder, clock, deployment.getTimeout(), deployment.getSleepBetweenRetries());
	}

	@Bean
	Deployer deployer(CentralPublishProperties properties, Logger logger, FileScanner fileScanner,
			ChecksumCreator checksumCreator, Bundler bundler, CentralPortalApi centralPortalApi,
			ArtifactAwaiter artifactAwaiter) {
		CentralPublishProperties.Deployment deployment = properties.getDeployment();
		return Deployer.create(logger, properties.getDirectoryAsPath(), getPublishingType(deployment), fileScanner,
				checksumCreator, bundler, centralPortalApi, artifactAwaiter, deployment.isDropOnFailure(),
				deployment.isIgnoreAlreadyExistsError(), getAwaitArtifact(deployment), deployment.getName());
	}

	@Bean
	ArtifactAwaiter artifactAwaiter(CentralPublishProperties properties, Logger logger,
			RestClient.Builder restClientBuilder) {
		return ArtifactAwaiter.create(logger, properties.getDeployment().getTimeout(),
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

	private boolean runsOnGithubActions() {
		return Boolean.parseBoolean(System.getenv("GITHUB_ACTIONS"));
	}

}
