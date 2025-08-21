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

import org.springframework.lang.Nullable;

/**
 * Deployment errors.
 *
 * @author Moritz Halbritter
 */
public class Errors {

	private final Map<Object, Object> errors;

	Errors(@Nullable Map<Object, Object> errors) {
		this.errors = (errors != null) ? errors : Collections.emptyMap();
	}

	/**
	 * Whether the errors contain a 'Deployment already exists' error.
	 * @return whether the errors contain a 'Deployment already exists' error
	 */
	@SuppressWarnings("unchecked")
	public boolean hasAlreadyExistsError() {
		for (Map.Entry<Object, Object> entry : this.errors.entrySet()) {
			if (entry.getKey() instanceof String key && entry.getValue() instanceof List) {
				List<String> values = (List<String>) entry.getValue();
				if (values.size() == 1) {
					String text = values.getFirst();
					String expectedText = "Component with package url: '%s' already exists".formatted(key);
					if (text != null && text.equals(expectedText)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return this.errors.toString();
	}

	static Errors empty() {
		return new Errors(Collections.emptyMap());
	}

}
