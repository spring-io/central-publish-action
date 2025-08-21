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

package io.spring.github.actions.centralpublish.sonatype;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.lang.Nullable;

/**
 * Deployment errors.
 *
 * @author Moritz Halbritter
 */
public class Errors {

	private final Map<Object, Object> errors;

	Errors(@Nullable Map<Object, Object> errors) {
		this.errors = (errors != null) ? new TreeMap<>(errors) : Collections.emptyMap();
	}

	/**
	 * Whether the errors contain only 'Deployment already exists' errors.
	 * @return whether the errors contain only 'Deployment already exists' errors
	 */
	@SuppressWarnings("unchecked")
	public boolean hasOnlyAlreadyExistsError() {
		if (this.errors.isEmpty()) {
			return false;
		}
		for (Map.Entry<Object, Object> entry : this.errors.entrySet()) {
			if (entry.getKey() instanceof String key && entry.getValue() instanceof List) {
				List<Object> values = (List<Object>) entry.getValue();
				if (values.size() != 1) {
					return false;
				}
				if (values.getFirst() instanceof String value) {
					if (!isAlreadyExistsError(key, value)) {
						return false;
					}
				}
				else {
					return false;
				}
			}
			else {
				return false;
			}
		}
		return true;
	}

	private boolean isAlreadyExistsError(String pkg, @Nullable String text) {
		String expectedText = "Component with package url: '%s' already exists".formatted(pkg);
		return text != null && text.equals(expectedText);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<Object, Object> entry : this.errors.entrySet()) {
			Object key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof Iterable<?> values) {
				builder.append("- %s:%n".formatted(key));
				for (Object v : values) {
					builder.append("  - %s%n".formatted(v));
				}
			}
			else {
				builder.append("- %s: %s%n".formatted(key, value));
			}
		}
		return builder.toString();
	}

	static Errors empty() {
		return new Errors(Collections.emptyMap());
	}

}
