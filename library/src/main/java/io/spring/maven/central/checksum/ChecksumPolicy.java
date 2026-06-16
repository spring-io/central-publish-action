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

package io.spring.maven.central.checksum;

import java.nio.file.Path;

/**
 * Policy for handling existing checksums.
 *
 * @author Moritz Halbritter
 */
public enum ChecksumPolicy {

	/**
	 * Fails if existing checksums are found.
	 */
	FAIL_ON_EXISTING {
		@Override
		void checksumExists(Path checksumFile) {
			throw new IllegalStateException(
					"Checksum file '%s' already exists'".formatted(checksumFile.toAbsolutePath()));
		}
	},
	/**
	 * Overwrite existing checksums.
	 */
	OVERWRITE_EXISTING {
		@Override
		void checksumExists(Path checksumFile) {
			// noop
		}
	};

	abstract void checksumExists(Path checksumFile);

}
