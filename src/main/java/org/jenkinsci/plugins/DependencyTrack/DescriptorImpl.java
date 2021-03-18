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

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.verb.POST;

/**
 * <p>
 * Descriptor for {@link DependencyTrackPublisher}. Used as a singleton. The
 * class is marked as public so that it can be accessed from views.
 * <p>
 * See
 * <code>src/main/resources/org/jenkinsci/plugins/DependencyCheck/DependencyTrackBuilder/*.jelly</code>
 * for the actual HTML fragment for the configuration screen.
 */
@Extension
@Symbol("dependencyTrackPublisher") // This indicates to Jenkins that this is an implementation of an extension point.
public final class DescriptorImpl extends BuildStepDescriptor<Publisher> implements Serializable {

    private static final long serialVersionUID = -2018722914973282748L;

    private transient final ApiClientFactory clientFactory;

    /**
     * Specifies the base URL to Dependency-Track v3 or higher.
     */
    @Setter(onMethod_ = {@DataBoundSetter})
    private String dependencyTrackUrl;

    /**
     * Specifies the alternative base URL to the frontend of Dependency-Track v3 or higher.
     */
    @Setter(onMethod_ = {@DataBoundSetter})
    private String dependencyTrackFrontendUrl;

    /**
     * Specifies an API Key used for authentication (if authentication is
     * required).
     */
    @Getter(onMethod_ = {@CheckForNull})
    @Setter(onMethod_ = {@DataBoundSetter})
    private String dependencyTrackApiKey;

    /**
     * Specifies whether the API key provided has the PROJECT_CREATION_UPLOAD
     * permission.
     */
    @Getter
    @Setter(onMethod_ = {@DataBoundSetter})
    private boolean dependencyTrackAutoCreateProjects;

    /**
     * Specifies the maximum number of minutes to wait for synchronous jobs to
     * complete.
     */
    @Setter(onMethod_ = {@DataBoundSetter})
    private int dependencyTrackPollingTimeout;

    /**
     * Defines the number of seconds to wait between two checks for
     * Dependency-Track to process a job (Synchronous Publishing Mode).
     */
    @Setter(onMethod_ = {@DataBoundSetter})
    private int dependencyTrackPollingInterval;

    /**
     * the connection-timeout in seconds for every call to DT
     */
    @Getter
    @Setter(onMethod_ = {@DataBoundSetter})
    private int dependencyTrackConnectionTimeout;

    /**
     * the read-timeout in seconds for every call to DT
     */
    @Getter
    @Setter(onMethod_ = {@DataBoundSetter})
    private int dependencyTrackReadTimeout;

    /**
     * Default constructor. Obtains the Descriptor used in
     * DependencyCheckBuilder as this contains the global Dependency-Check
     * Jenkins plugin configuration.
     */
    public DescriptorImpl() {
        this(ApiClient::new);
    }

    DescriptorImpl(@NonNull ApiClientFactory clientFactory) {
        super(DependencyTrackPublisher.class);
        this.clientFactory = clientFactory;
        load();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        // Indicates that this builder can be used with all kinds of project types
        return true;
    }

    /**
     * Retrieve the projects to populate the dropdown.
     *
     * @param dependencyTrackUrl the base URL to Dependency-Track
     * @param dependencyTrackApiKey the API key to use for authentication
     * @param item used to lookup credentials in job config. ignored in global
     * @return ListBoxModel
     */
    @POST
    public ListBoxModel doFillProjectIdItems(@QueryParameter final String dependencyTrackUrl, @QueryParameter final String dependencyTrackApiKey, @AncestorInPath @Nullable Item item) {
        final ListBoxModel projects = new ListBoxModel();
        try {
            // url may come from instance-config. if empty, then take it from global config (this)
            final String url = Optional.ofNullable(PluginUtil.parseBaseUrl(dependencyTrackUrl)).orElse(getDependencyTrackUrl());
            // api-key may come from instance-config. if empty, then take it from global config (this)
            final String apiKey = lookupApiKey(Optional.ofNullable(StringUtils.trimToNull(dependencyTrackApiKey)).orElse(getDependencyTrackApiKey()), item);
            final ApiClient apiClient = getClient(url, apiKey);
            projects.addAll(apiClient.getProjects().stream()
                    .map(p -> new ListBoxModel.Option(p.getName().concat(" ").concat(Optional.ofNullable(p.getVersion()).orElse(StringUtils.EMPTY)).trim(), p.getUuid()))
                    .sorted(Comparator.comparing(o -> o.name))
                    .collect(Collectors.toList())
            );
            projects.add(0, new ListBoxModel.Option("-- Select Project --", null));
        } catch (ApiClientException e) {
            projects.add(Messages.Builder_Error_Projects(e.getLocalizedMessage()), null);
        }
        return projects;
    }

    @POST
    public ListBoxModel doFillDependencyTrackApiKeyItems(@QueryParameter String credentialsId, @AncestorInPath Item item) {
        StandardListBoxModel result = new StandardListBoxModel();
        if (item == null) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return result.includeCurrentValue(credentialsId);
            }
        } else {
            if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return result.includeCurrentValue(credentialsId);
            }
        }
        return result
                .includeEmptyValue()
                .includeAs(ACL.SYSTEM, item, StringCredentials.class, Collections.emptyList())
                .includeCurrentValue(credentialsId);
    }

    /**
     * Performs input validation when submitting the global config
     *
     * @param value The value of the URL as specified in the global config
     * @param item used to check permissions in job config. ignored in global
     * @return a FormValidation object
     */
    @POST
    public FormValidation doCheckDependencyTrackUrl(@QueryParameter String value, @AncestorInPath @Nullable Item item) {
        if (item == null) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        } else {
            item.checkPermission(Item.CONFIGURE);
        }
        return PluginUtil.doCheckUrl(value);
    }

    /**
     * Performs input validation when submitting the global config
     *
     * @param value The value of the URL as specified in the global config
     * @param item used to check permissions in job config. ignored in global
     * @return a FormValidation object
     */
    @POST
    public FormValidation doCheckDependencyTrackFrontendUrl(@QueryParameter String value, @AncestorInPath @Nullable Item item) {
        if (item == null) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        } else {
            item.checkPermission(Item.CONFIGURE);
        }
        return PluginUtil.doCheckUrl(value);
    }

    /**
     * Performs an on-the-fly check of the Dependency-Track URL and api key
     * parameters by making a simple call to the server and validating the
     * response code.
     *
     * @param dependencyTrackUrl the base URL to Dependency-Track
     * @param dependencyTrackApiKey the credential-id of the API key to use for authentication
     * @param item used to lookup credentials in job config. ignored in global
     * config
     * @return FormValidation
     */
    @POST
    public FormValidation doTestConnection(@QueryParameter final String dependencyTrackUrl, @QueryParameter final String dependencyTrackApiKey, @AncestorInPath @Nullable Item item) {
        if (item == null) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        } else {
            item.checkPermission(Item.CONFIGURE);
        }
        // url may come from instance-config. if empty, then take it from global config (this)
        final String url = Optional.ofNullable(PluginUtil.parseBaseUrl(dependencyTrackUrl)).orElse(getDependencyTrackUrl());
        // api-key may come from instance-config. if empty, then take it from global config (this)
        final String apiKey = lookupApiKey(Optional.ofNullable(StringUtils.trimToNull(dependencyTrackApiKey)).orElse(getDependencyTrackApiKey()), item);
        if (doCheckDependencyTrackUrl(url, item).kind == FormValidation.Kind.OK && StringUtils.isNotBlank(apiKey)) {
            try {
                final ApiClient apiClient = getClient(url, apiKey);
                final String result = apiClient.testConnection();
                return result.startsWith("Dependency-Track v") ? FormValidation.ok("Connection successful - " + result) : FormValidation.error("Connection failed - " + result);
            } catch (ApiClientException e) {
                return FormValidation.error(e, "Connection failed");
            }
        }
        return FormValidation.warning("URL must be valid and Api-Key must not be empty");
    }

    /**
     * Takes the /apply/save step in the global config and saves the JSON data.
     *
     * @param req the request
     * @param formData the form data
     * @return a boolean
     * @throws FormException an exception validating form input
     */
    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
        req.bindJSON(this, formData);
        save();
        return super.configure(req, formData);
    }

    /**
     * This name is used on the build configuration screen.
     *
     * @return
     */
    @Override
    public String getDisplayName() {
        return Messages.Publisher_DependencyTrack_Name();
    }

    /**
     * @return global configuration for dependencyTrackUrl
     */
    @CheckForNull
    public String getDependencyTrackUrl() {
        return PluginUtil.parseBaseUrl(dependencyTrackUrl);
    }

    /**
     * @return global configuration for dependencyTrackFrontendUrl
     */
    @CheckForNull
    public String getDependencyTrackFrontendUrl() {
        return PluginUtil.parseBaseUrl(dependencyTrackFrontendUrl);
    }

    /**
     * @return global configuration for dependencyTrackPollingTimeout.
     */
    public int getDependencyTrackPollingTimeout() {
        if (dependencyTrackPollingTimeout <= 0) {
            return 5;
        }
        return dependencyTrackPollingTimeout;
    }

    /**
     * @return global configuration for dependencyTrackPollingInterval.
     */
    public int getDependencyTrackPollingInterval() {
        if (dependencyTrackPollingInterval <= 0) {
            return 10;
        }
        return dependencyTrackPollingInterval;
    }

    private ApiClient getClient(final String baseUrl, final String apiKey) {
        return clientFactory.create(baseUrl, apiKey, new ConsoleLogger(), Math.max(dependencyTrackConnectionTimeout, 0), Math.max(dependencyTrackReadTimeout, 0));
    }

    private String lookupApiKey(final String credentialId, final Item item) {
        return CredentialsProvider.lookupCredentials(StringCredentials.class, item, ACL.SYSTEM, Collections.emptyList()).stream()
                .filter(c -> c.getId().equals(credentialId))
                .map(StringCredentials::getSecret)
                .map(Secret::getPlainText)
                .findFirst().orElse(StringUtils.EMPTY);
    }
}
