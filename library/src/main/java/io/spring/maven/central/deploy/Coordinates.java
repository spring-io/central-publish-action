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

import org.springframework.util.Assert;

/**
 * Dependency coordinates.
 *
 * @param group the group
 * @param artifact the artifact
 * @param version the version
 * @author Moritz Halbritter
 */
public record Coordinates(String group, String artifact, String version) {

	/**
	 * Parses the given coordinate string.
	 * @param input the coordinate string to parse, in the format
	 * {@code group:artifact:version}
	 * @return the parsed {@link Coordinates}
	 * @throws IllegalStateException if the coordinate string does not contain exactly 3
	 * parts
	 */
	public static Coordinates parse(String input) {
		String[] parts = input.split(":");
		Assert.state(parts.length == 3, "Expected 3 parts, got %d for '%s'".formatted(parts.length, input));
		return new Coordinates(parts[0], parts[1], parts[2]);
	}

	@Override
	public String toString() {
		return "%s:%s:%s".formatted(this.group, this.artifact, this.version);
	}
}
