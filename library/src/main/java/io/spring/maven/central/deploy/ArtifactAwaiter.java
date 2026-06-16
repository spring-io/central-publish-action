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

import java.net.URI;
import java.time.Duration;

import io.spring.maven.central.log.Logger;

import org.springframework.web.client.RestClient;

/**
 * Awaits until an artifact is available.
 *
 * @author Moritz Halbritter
 */
public interface ArtifactAwaiter {

	/**
	 * Waits until the given artifact is available.
	 * @param coordinates the coordinates
	 * @throws ArtifactAwaitException if the timeout is reached before the artifact is
	 * available
	 */
	void await(Coordinates coordinates);

	/**
	 * Creates a new {@link ArtifactAwaiter}.
	 * @param logger the logger
	 * @param timeout the timeout
	 * @param sleepBetweenRetries the duration to sleep between retries
	 * @param baseUri the base URI of the repository in which the artifact is stored
	 * @param restClientBuilder the builder for a {@link RestClient}
	 * @return the {@link ArtifactAwaiter}
	 */
	static ArtifactAwaiter create(Logger logger, Duration timeout, Duration sleepBetweenRetries, URI baseUri,
			RestClient.Builder restClientBuilder) {
		return new ArtifactAwaiterImpl(logger, timeout, sleepBetweenRetries, baseUri, restClientBuilder);
	}

}
