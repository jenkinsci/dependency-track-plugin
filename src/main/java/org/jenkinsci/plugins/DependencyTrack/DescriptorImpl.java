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

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.Serializable;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

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
     * Specifies an API Key used for authentication (if authentication is
     * required).
     */
    @Getter
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
     * @return ListBoxModel
     */
    public ListBoxModel doFillProjectIdItems() {
        final ListBoxModel projects = new ListBoxModel();
        projects.add("-- Select Project --", null);
        try {
            final ApiClient apiClient = getClient(dependencyTrackUrl, dependencyTrackApiKey);
            apiClient.getProjects().forEach(p -> {
                String displayName = StringUtils.isNotBlank(p.getVersion()) ? p.getName() + " " + p.getVersion() : p.getName();
                projects.add(displayName, p.getUuid());
            });
        } catch (ApiClientException e) {
            projects.clear();
            projects.add(Messages.Builder_Error_Projects() + ": " + e.getLocalizedMessage(), null);
        }
        return projects;
    }

    /**
     * Performs input validation when submitting the global config
     *
     * @param value The value of the URL as specified in the global config
     * @return a FormValidation object
     */
    public FormValidation doCheckDependencyTrackUrl(@QueryParameter String value) {
        return PluginUtil.doCheckUrl(value);
    }

    /**
     * Performs an on-the-fly check of the Dependency-Track URL and api key
     * parameters by making a simple call to the server and validating the
     * response code.
     *
     * @param dependencyTrackUrl the base URL to Dependency-Track
     * @param dependencyTrackApiKey the API key to use for authentication
     * @return FormValidation
     */
    public FormValidation doTestConnection(@QueryParameter final String dependencyTrackUrl,
            @QueryParameter final String dependencyTrackApiKey) {
        try {
            final ApiClient apiClient = getClient(dependencyTrackUrl, dependencyTrackApiKey);
            return FormValidation.ok("Connection successful - " + apiClient.testConnection());
        } catch (ApiClientException e) {
            return FormValidation.error(e, "Connection failed");
        }
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
        dependencyTrackUrl = formData.getString("dependencyTrackUrl");
        dependencyTrackApiKey = formData.getString("dependencyTrackApiKey");
        dependencyTrackAutoCreateProjects = formData.getBoolean("dependencyTrackAutoCreateProjects");
        dependencyTrackPollingTimeout = formData.getInt("dependencyTrackPollingTimeout");
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
    public String getDependencyTrackUrl() {
        return PluginUtil.parseBaseUrl(dependencyTrackUrl);
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
    
    private ApiClient getClient(final String baseUrl, final String apiKey) {
        return clientFactory.create(baseUrl, apiKey, new ConsoleLogger());
    }
}
