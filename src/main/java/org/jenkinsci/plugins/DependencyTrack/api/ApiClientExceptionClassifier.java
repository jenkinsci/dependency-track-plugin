/*
 * Copyright 2024 OWASP.
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
package org.jenkinsci.plugins.DependencyTrack.api;

import java.io.IOException;
import java.util.List;
import org.springframework.classify.BinaryExceptionClassifier;

/**
 * custom classifier that classifies only {@link IOException} but excludes
 * {@link ApiClientException}s without any cause.
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
class ApiClientExceptionClassifier extends BinaryExceptionClassifier {

    public ApiClientExceptionClassifier() {
        super(List.of(IOException.class), true);
    }

    @Override
    public Boolean classify(Throwable classifiable) {
        // pure ApiClientException without any cause will be classified as false
        return super.classify(classifiable) && !(classifiable instanceof ApiClientException && classifiable.getCause() == null);
    }

}
