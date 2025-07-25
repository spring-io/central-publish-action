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

package io.spring.github.actions.centralpublish.portalmock.deployment;

import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@link RestController} to inspect and download published deployments.
 *
 * @author Moritz Halbritter
 */
@RestController
@RequestMapping(path = "/debug/published-deployments", produces = MediaType.APPLICATION_JSON_VALUE)
class PublishedDeploymentsController {

	private final PublishedDeployments publishedDeployments;

	PublishedDeploymentsController(PublishedDeployments publishedDeployments) {
		this.publishedDeployments = publishedDeployments;
	}

	@GetMapping
	List<PublishedDeploymentDto> listAll() {
		return this.publishedDeployments.list()
			.stream()
			.map((e) -> new PublishedDeploymentDto(e.getId(), e.getBundle().toString()))
			.toList();
	}

	@GetMapping(path = "/{id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	ResponseEntity<Resource> get(@PathVariable("id") String id) {
		Deployment deployment = this.publishedDeployments.get(id);
		if (deployment == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(new FileSystemResource(deployment.getBundle()));
	}

	@GetMapping(path = "/file/{*name}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	ResponseEntity<byte[]> getFile(@PathVariable("name") String name) {
		if (name.startsWith("/")) {
			name = name.substring(1);
		}
		Deployment deployment = this.publishedDeployments.findWithFile(name);
		if (deployment == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(new byte[0]);
	}

	record PublishedDeploymentDto(String id, String file) {
	}

}
