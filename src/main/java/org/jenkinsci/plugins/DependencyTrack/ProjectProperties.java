/*
 * This file is part of Dependency-Track Jenkins plugin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.DependencyTrack;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import static org.jenkinsci.plugins.DependencyTrack.PluginUtil.areAllElementsOfType;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@lombok.NoArgsConstructor(onConstructor_ = {@DataBoundConstructor})
@EqualsAndHashCode(callSuper = false, doNotUseGetters = true)
public final class ProjectProperties extends AbstractDescribableImpl<ProjectProperties> implements Serializable {

    private static final long serialVersionUID = 5343757342998957784L;

    /**
     * Tags to set for the (new) project
     */
    private List<String> tags;

    @NonNull
    public List<String> getTags() {
        return normalizeTags(tags);
    }

    @DataBoundSetter
    @SuppressWarnings("unchecked")
    public void setTags(final Object value) {
        if (value instanceof String) {
            setTagsIntern((String) value);
        } else if (value instanceof String[]) {
            setTagsIntern((String[]) value);
        } else if (value instanceof Set && areAllElementsOfType((Set) value, String.class)) {
            setTagsIntern((Set<String>) value);
        } else if (value instanceof List && areAllElementsOfType((List) value, String.class)) {
            setTagsIntern((List<String>) value);
        } else {
            throw new IllegalArgumentException("expected String, String[], Set<String> or List<String> but got " + (value != null ? value.getClass().getName() : "null"));
        }
    }

    private void setTagsIntern(@NonNull final String value) {
        setTagsIntern(value.split("\\s+"));
    }

    private void setTagsIntern(@NonNull final String[] values) {
        setTagsIntern(Stream.of(values).collect(Collectors.toSet()));
    }

    private void setTagsIntern(@NonNull final Collection<String> values) {
        tags = normalizeTags(values);
    }

    @NonNull
    public String getTagsAsText() {
        return StringUtils.join(getTags(), System.lineSeparator());
    }

    @NonNull
    private List<String> normalizeTags(final Collection<String> values) {
        return (values != null ? values.stream() : Stream.<String>empty())
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(String::toLowerCase)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ProjectProperties> {
    }
}
