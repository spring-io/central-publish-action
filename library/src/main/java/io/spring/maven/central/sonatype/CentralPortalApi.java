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

package io.spring.maven.central.sonatype;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;

import io.spring.maven.central.bundle.Bundle;
import io.spring.maven.central.log.Logger;
import org.jspecify.annotations.Nullable;

import org.springframework.web.client.RestClient;

/**
 * Client for Sonatype's Central Portal API.
 *
 * @author Moritz Halbritter
 */
public interface CentralPortalApi {

	/**
	 * Uploads the given bundle using the given publishing type.
	 * @param bundle the bundle to upload
	 * @param publishingType the publishing type
	 * @param deploymentName the name of the deployment. If {@code null} or empty, a name
	 * will be generated.
	 * @return the deployment
	 */
	Deployment upload(Bundle bundle, PublishingType publishingType, @Nullable String deploymentName);

	/**
	 * Create a new {@link CentralPortalApi}.
	 * @param logger the logger
	 * @param baseUri the base URI of the Sonatype Central Portal
	 * @param tokenName the name of the token (username in basic auth)
	 * @param token the token (value in basic auth)
	 * @param restClientBuilder the builder for a {@link RestClient}
	 * @param clock the clock
	 * @param timeout the timeout for the operations
	 * @param sleepBetweenRetries the duration to sleep between the retries
	 * @return the {@link CentralPortalApi}
	 */
	static CentralPortalApi create(Logger logger, URI baseUri, String tokenName, String token,
			RestClient.Builder restClientBuilder, Clock clock, Duration timeout, Duration sleepBetweenRetries) {
		return new CentralPortalApiImpl(logger, baseUri, tokenName, token, restClientBuilder, clock, timeout,
				sleepBetweenRetries);
	}

}
