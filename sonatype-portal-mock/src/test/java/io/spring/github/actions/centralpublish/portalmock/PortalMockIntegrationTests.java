/*
 * Copyright 2025-2025 the original author or authors.
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

package io.spring.github.actions.centralpublish.portalmock;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import io.spring.github.actions.centralpublish.portalmock.deployment.StatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PortalMock}.
 *
 * @author Moritz Halbritter
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PortalMockIntegrationTests {

	private RestClient restClient;

	@BeforeEach
	void setUp(@LocalServerPort int port, @Autowired PortalMockProperties properties) {
		this.restClient = RestClient.builder()
			.baseUrl("http://localhost:" + port)
			.defaultHeader("Authorization", "Bearer " + base64Auth(properties.getToken()))
			.build();
	}

	@Test
	void test() throws Exception {
		String deploymentId = uploadBundle();
		StatusResponse status = waitForFinalStatus(deploymentId);
		assertThat(status.deploymentState()).isEqualTo("PUBLISHED");
		ResponseEntity<Void> deployment = this.restClient.get()
			.uri("/debug/published-deployments/{id}", deploymentId)
			.retrieve()
			.toBodilessEntity();
		assertThat(deployment.getStatusCode()).isEqualTo(HttpStatus.OK);
		ResponseEntity<Void> file = this.restClient.get()
			.uri("/debug/published-deployments/file/io/github/mhalbritter/sonatype-central-portal-test/0.0.5/sonatype-central-portal-test-0.0.5.jar")
			.retrieve()
			.toBodilessEntity();
		assertThat(file.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	private String base64Auth(PortalMockProperties.Token token) {
		return Base64.getEncoder()
			.encodeToString("%s:%s".formatted(token.getName(), token.getValue()).getBytes(StandardCharsets.UTF_8));
	}

	private StatusResponse waitForFinalStatus(String deploymentId) {
		while (true) {
			StatusResponse result = this.restClient.post()
				.uri("/api/v1/publisher/status?id=" + deploymentId)
				.retrieve()
				.body(StatusResponse.class);
			assertThat(result).isNotNull();
			if (isFinalStatus(result)) {
				return result;
			}
			sleep();
		}
	}

	private void sleep() {
		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException ex) {
			throw new IllegalStateException("Got interrupted while sleeping", ex);
		}
	}

	private boolean isFinalStatus(StatusResponse result) {
		return switch (result.deploymentState()) {
			case "PUBLISHED", "FAILED" -> true;
			default -> false;
		};
	}

	private String uploadBundle() {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("bundle", new ClassPathResource("bundle-with-checksums.zip"));
		return this.restClient.post().uri("/api/v1/publisher/upload").body(body).retrieve().body(String.class);
	}

}
