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

import java.io.Serial;
import java.net.URI;
import java.time.Duration;

import io.spring.github.actions.centralpublish.system.Logger;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * Waits until an artifact is available.
 *
 * @author Moritz Halbritter
 */
class ArtifactAwaiter {

	private final Logger logger;

	private final Duration timeout;

	private final Duration sleepBetweenRetries;

	private final RestClient restClient;

	/**
	 * Creates a new instance.
	 * @param logger the logger
	 * @param timeout the timeout
	 * @param sleepBetweenRetries the duration to sleep between retries
	 * @param baseUri the base uri
	 * @param restClientBuilder the builder for the {@link RestClient}
	 */
	ArtifactAwaiter(Logger logger, Duration timeout, Duration sleepBetweenRetries, URI baseUri,
			RestClient.Builder restClientBuilder) {
		this.logger = logger;
		this.timeout = timeout;
		this.sleepBetweenRetries = sleepBetweenRetries;
		this.restClient = restClientBuilder.baseUrl(baseUri).build();
	}

	void await(Coordinates coordinates) {
		long start = System.nanoTime();
		String url = groupToPath(coordinates) + "/" + coordinates.artifact() + "/" + coordinates.version() + "/"
				+ coordinates.artifact() + "-" + coordinates.version() + ".jar";
		this.logger.debug("Awaiting artifact at {}", url);
		while (true) {
			checkTimeout(start, coordinates, url);
			ResponseEntity<Void> result = this.restClient.head()
				.uri(url)
				.retrieve()
				.onStatus((status) -> true, (req, res) -> {
				})
				.toBodilessEntity();
			this.logger.debug("Got {}", result.getStatusCode().value());
			if (result.getStatusCode().is2xxSuccessful()) {
				return;
			}
			sleep();
		}
	}

	private void sleep() {
		try {
			Thread.sleep(this.sleepBetweenRetries.toMillis());
		}
		catch (InterruptedException ex) {
			throw new IllegalStateException("Got interrupted while sleeping", ex);
		}
	}

	private void checkTimeout(long start, Coordinates coordinates, String url) {
		Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
		if (elapsed.compareTo(this.timeout) > 0) {
			throw new ArtifactAwaitException("Timeout of %s reached while waiting for artifact %s at url '%s'"
				.formatted(this.timeout, coordinates, url));
		}
	}

	private String groupToPath(Coordinates coordinates) {
		return coordinates.group().replace('.', '/');
	}

	static class ArtifactAwaitException extends RuntimeException {

		@Serial
		private static final long serialVersionUID = 1L;

		ArtifactAwaitException(String message) {
			super(message);
		}

	}

}
