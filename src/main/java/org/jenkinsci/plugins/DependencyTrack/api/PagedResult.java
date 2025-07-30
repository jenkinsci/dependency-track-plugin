/*
 * Copyright 2025 OWASP.
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

import java.util.List;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
public final record PagedResult<T>(
        /**
         * the result for the current page
         */
        List<T> result,
        /**
         * the total amount of results
         */
        int totalSize
        ) {

    public static final PagedResult<?> EMPTY = new PagedResult<>(List.of(), 0);

    public PagedResult(List<T> result, int totalSize) {
        this.result = result != null ? List.copyOf(result) : List.of();
        this.totalSize = totalSize;
    }

    /**
     * return the size of the current result. this is actually just a shortcut
     * for {@code result().size()}.
     *
     * @return the size of the current result
     * @see List#size()
     */
    public int size() {
        return result.size();
    }

    /**
     * returns {@code true} if the current result is empty. this is actually
     * just a shortcut for {@code result().isEmpty()}.
     *
     * @return {@code true} if the current result is empty
     * @see List#isEmpty()
     */
    public boolean isEmpty() {
        return result.isEmpty();
    }
}
