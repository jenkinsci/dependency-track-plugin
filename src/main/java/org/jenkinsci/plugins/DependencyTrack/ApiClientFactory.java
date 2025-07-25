/*
 * Copyright 2020 OWASP.
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
package org.jenkinsci.plugins.DependencyTrack;

import okhttp3.OkHttpClient;
import org.jenkinsci.plugins.DependencyTrack.api.ApiClient;
import org.jenkinsci.plugins.DependencyTrack.api.Logger;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@FunctionalInterface
interface ApiClientFactory {

    ApiClient create(final String baseUrl, final String apiKey, final Logger logger, final OkHttpClient httpClient);
}
