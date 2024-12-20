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
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.RelativePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.ListBoxModel;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jenkins.model.Jenkins;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import static org.jenkinsci.plugins.DependencyTrack.PluginUtil.areAllElementsOfType;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@Getter
@lombok.NoArgsConstructor(onConstructor_ = {@DataBoundConstructor})
@EqualsAndHashCode(callSuper = false, doNotUseGetters = true)
public final class ProjectProperties extends AbstractDescribableImpl<ProjectProperties> implements Serializable {

    private static final long serialVersionUID = 5343757342998957784L;

    /**
     * Tags to set for the project
     */
    @Nullable
    private List<String> tags;

    /**
     * SWID Tag ID for the project
     */
    @Nullable
    private String swidTagId;
    
    /**
     * Group to set for the project
     */
    @Nullable
    private String group;
    
    /**
     * Description to set for the project
     */
    @Nullable
    private String description;
    
    /**
     * UUID of the parent project
     */
    @Nullable
    private String parentId;

    /**
     * Name of the parent project
     */
    @Nullable
    private String parentName;

    /**
     * Version of the parent project
     */
    @Nullable
    private String parentVersion;

    /**
     * Mark this version of the project as the latest version
     */
    @Nullable
    @Setter(onMethod_ = {@DataBoundSetter})
    private Boolean isLatest;

    @NonNull
    public List<String> getTags() {
        return normalizeTags(tags);
    }

    @DataBoundSetter
    @SuppressWarnings("unchecked")
    public void setTags(final Object value) {
        if (value instanceof String string) {
            setTagsIntern(string);
        } else if (value instanceof String[] strings) {
            setTagsIntern(strings);
        } else if (value instanceof Collection collection && areAllElementsOfType(collection, String.class)) {
            setTagsIntern(collection);
        } else if (value == null) {
            tags = null;
        } else {
            throw new IllegalArgumentException("expected String, String[], Set<String> or List<String> but got " + value.getClass().getName());
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

    @DataBoundSetter
    public void setSwidTagId(final String swidTagId) {
        this.swidTagId = StringUtils.trimToNull(swidTagId);
    }

    @DataBoundSetter
    public void setGroup(final String group) {
        this.group = StringUtils.trimToNull(group);
    }

    @DataBoundSetter
    public void setDescription(final String description) {
        this.description = StringUtils.trimToNull(description);
    }

    @DataBoundSetter
    public void setParentId(final String parentId) {
        this.parentId = StringUtils.trimToNull(parentId);
    }

    @DataBoundSetter
    public void setParentName(final String parentName) {
        this.parentName = StringUtils.trimToNull(parentName);
    }

    @DataBoundSetter
    public void setParentVersion(final String parentVersion) {
        this.parentVersion = StringUtils.trimToNull(parentVersion);
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
                // list must not be immutable:
                // java.lang.UnsupportedOperationException: Refusing to marshal java.util.ImmutableCollections$ListN for security reasons
                .collect(Collectors.toList());
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ProjectProperties> {

        /**
         * Retrieve the projects to populate the dropdown.
         *
         * @param dependencyTrackUrl the base URL to Dependency-Track
         * @param dependencyTrackApiKey the API key to use for authentication
         * @param item used to lookup credentials in job config
         * @return ListBoxModel
         */
        @POST
        public ListBoxModel doFillParentIdItems(@RelativePath("..") @QueryParameter final String dependencyTrackUrl, @RelativePath("..") @QueryParameter final String dependencyTrackApiKey, @AncestorInPath @Nullable final Item item) {
            org.jenkinsci.plugins.DependencyTrack.DescriptorImpl pluginDescriptor = Jenkins.get().getDescriptorByType(org.jenkinsci.plugins.DependencyTrack.DescriptorImpl.class);
            return pluginDescriptor.doFillProjectIdItems(dependencyTrackUrl, dependencyTrackApiKey, item);
        }
    }
}
