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
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import hudson.util.VersionNumber;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.DependencyTrack.model.Team;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.verb.POST;

import static org.jenkinsci.plugins.DependencyTrack.model.Permissions.*;

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
public class DescriptorImpl extends BuildStepDescriptor<Publisher> implements Serializable {

    private static final long serialVersionUID = -2018722914973282748L;

    private final transient ApiClientFactory clientFactory;

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
    public ListBoxModel doFillProjectIdItems(@QueryParameter final String dependencyTrackUrl, @QueryParameter final String dependencyTrackApiKey, @AncestorInPath @Nullable final Item item) {
        final ListBoxModel projects = new ListBoxModel();
        try {
            // url may come from instance-config. if empty, then take it from global config (this)
            final String url = Optional.ofNullable(PluginUtil.parseBaseUrl(dependencyTrackUrl)).orElseGet(this::getDependencyTrackUrl);
            // api-key may come from instance-config. if empty, then take it from global config (this)
            final String apiKey = lookupApiKey(Optional.ofNullable(StringUtils.trimToNull(dependencyTrackApiKey)).orElseGet(this::getDependencyTrackApiKey), item);
            final ApiClient apiClient = getClient(url, apiKey);
            final List<ListBoxModel.Option> options = apiClient.getProjects().stream()
                    .map(p -> new ListBoxModel.Option(p.getName().concat(" ").concat(Optional.ofNullable(p.getVersion()).orElse(StringUtils.EMPTY)).trim(), p.getUuid()))
                    .sorted(Comparator.comparing(o -> o.name))
                    .collect(Collectors.toList());
            projects.add(new ListBoxModel.Option(Messages.Publisher_ProjectList_Placeholder(), StringUtils.EMPTY));
            projects.addAll(options);
        } catch (ApiClientException e) {
            projects.add(Messages.Builder_Error_Projects(e.getLocalizedMessage()), StringUtils.EMPTY);
        }
        return projects;
    }

    @POST
    public ListBoxModel doFillDependencyTrackApiKeyItems(@QueryParameter final String credentialsId, @AncestorInPath final Item item) {
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
                .includeAs(ACL.SYSTEM, item, StringCredentials.class, List.of())
                .includeCurrentValue(credentialsId);
    }

    /**
     * Performs input validation when submitting the global or job config
     *
     * @param value The value of the URL as specified in the global config
     * @param item used to check permissions in job config. ignored in global
     * @return a FormValidation object
     */
    @POST
    public FormValidation doCheckDependencyTrackUrl(@QueryParameter final String value, @AncestorInPath @Nullable final Item item) {
        if (item == null) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        } else {
            item.checkPermission(Item.CONFIGURE);
        }
        return PluginUtil.doCheckUrl(value);
    }

    /**
     * Performs input validation when submitting the global or job config
     *
     * @param value The value of the URL as specified in the global config
     * @param item used to check permissions in job config. ignored in global
     * @return a FormValidation object
     */
    @POST
    public FormValidation doCheckDependencyTrackFrontendUrl(@QueryParameter final String value, @AncestorInPath @Nullable final Item item) {
        return doCheckDependencyTrackUrl(value, item);
    }

    @POST
    public FormValidation doTestConnectionGlobal(@QueryParameter final String dependencyTrackUrl, @QueryParameter final String dependencyTrackApiKey, @QueryParameter final boolean dependencyTrackAutoCreateProjects, @AncestorInPath @Nullable Item item) {
        return testConnection(dependencyTrackUrl, dependencyTrackApiKey, dependencyTrackAutoCreateProjects, false, false, item);
    }

    @POST
    public FormValidation doTestConnectionJob(@QueryParameter final String dependencyTrackUrl, @QueryParameter final String dependencyTrackApiKey, @QueryParameter final boolean autoCreateProjects, @QueryParameter final boolean synchronous, @QueryParameter final boolean projectProperties, @AncestorInPath @Nullable Item item) {
        return testConnection(dependencyTrackUrl, dependencyTrackApiKey, autoCreateProjects, synchronous, projectProperties, item);
    }

    /**
     * Performs an on-the-fly check of the Dependency-Track URL and api key
     * parameters by making a simple call to the server and validating the
     * response code.
     *
     * @param dependencyTrackUrl the base URL to Dependency-Track
     * @param dependencyTrackApiKey the credential-id of the API key to use for
     * authentication
     * @param autoCreateProjects if auto-create projects is enabled or not
     * @param synchronous if sync-mode is enabled or not
     * @param item used to check permission and lookup credentials
     * @return FormValidation
     */
    private FormValidation testConnection(final String dependencyTrackUrl, final String dependencyTrackApiKey, final boolean autoCreateProjects, final boolean synchronous, final boolean updateProjectProperties, @AncestorInPath @Nullable Item item) {
        if (item == null) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        } else {
            item.checkPermission(Item.CONFIGURE);
        }
        // url may come from instance-config. if empty, then take it from global config (this)
        final String url = Optional.ofNullable(PluginUtil.parseBaseUrl(dependencyTrackUrl)).orElseGet(this::getDependencyTrackUrl);
        // api-key may come from instance-config. if empty, then take it from global config (this)
        final String apiKey = lookupApiKey(Optional.ofNullable(StringUtils.trimToNull(dependencyTrackApiKey)).orElseGet(this::getDependencyTrackApiKey), item);
        if (doCheckDependencyTrackUrl(url, item).kind == FormValidation.Kind.OK && StringUtils.isNotBlank(apiKey)) {
            try {
                final ApiClient apiClient = getClient(url, apiKey);
                final var poweredBy = apiClient.testConnection();
                if (!poweredBy.startsWith("Dependency-Track v")) {
                    return FormValidation.error(Messages.Publisher_ConnectionTest_Error(poweredBy));
                }
                final VersionNumber version = apiClient.getVersion();
                final var requiredVersion = new VersionNumber("4.7.0");
                if (version.isOlderThan(requiredVersion)) {
                    return FormValidation.error(Messages.Publisher_ConnectionTest_VersionWarning(version, requiredVersion));
                }
                return checkTeamPermissions(apiClient, poweredBy, autoCreateProjects, synchronous, updateProjectProperties);
            } catch (ApiClientException e) {
                return FormValidation.error(e, Messages.Publisher_ConnectionTest_Error(e.getMessage()));
            }
        }
        return FormValidation.error(Messages.Publisher_ConnectionTest_InputError());
    }

    private FormValidation checkTeamPermissions(final ApiClient apiClient,
                                                final String poweredBy,
                                                final boolean autoCreateProjects,
                                                final boolean synchronous,
                                                final boolean projectProperties) throws ApiClientException
    {
        final Set<String> requiredPermissions = Stream.of(BOM_UPLOAD, VIEW_PORTFOLIO, VULNERABILITY_ANALYSIS).map(Enum::toString).collect(Collectors.toSet());
        final Set<String> optionalPermissions = new HashSet<>();

        if (autoCreateProjects) {
            requiredPermissions.add(PROJECT_CREATION_UPLOAD.toString());
        } else {
            optionalPermissions.add(PROJECT_CREATION_UPLOAD.toString());
        }
        if (synchronous) {
            requiredPermissions.add(VIEW_VULNERABILITY.toString());
        } else {
            optionalPermissions.add(VIEW_VULNERABILITY.toString());
        }

        optionalPermissions.add(VIEW_POLICY_VIOLATION.toString());

        if (projectProperties) {
            requiredPermissions.add(PORTFOLIO_MANAGEMENT.toString());
        } else {
            optionalPermissions.add(PORTFOLIO_MANAGEMENT.toString());
        }

        final Team team = apiClient.getTeamPermissions();
        final Set<String> allPermissions = new TreeSet<>(team.getPermissions());
        allPermissions.addAll(requiredPermissions);
        allPermissions.addAll(optionalPermissions);
        final var sb = new StringBuilder();
        sb.append("<p class=\"team\">");
        sb.append(Messages.Publisher_PermissionTest_Team(Util.escape(team.getName())));
        sb.append("</p><ul>");
        FormValidation.Kind worst = FormValidation.Kind.OK;
        for (String perm : allPermissions) {
            String cssClass = "optional";
            FormValidation.Kind kind = FormValidation.Kind.OK;
            String message = Messages.Publisher_PermissionTest_Optional(Util.escape(perm));
            if (requiredPermissions.contains(perm)) {
                cssClass = team.getPermissions().contains(perm) ? "okay" : "missing";
                kind = team.getPermissions().contains(perm) ? FormValidation.Kind.OK : FormValidation.Kind.ERROR;
                message = team.getPermissions().contains(perm) ? Messages.Publisher_PermissionTest_Okay(Util.escape(perm)) : Messages.Publisher_PermissionTest_Missing(Util.escape(perm));
            } else {
                if (!optionalPermissions.contains(perm)) {
                    cssClass = "warn";
                    kind = FormValidation.Kind.WARNING;
                    message = Messages.Publisher_PermissionTest_Warning(Util.escape(perm));
                }
            }
            sb.append(String.format("<li class=\"permission %s\">%s</li>", cssClass, message));
            if (kind.ordinal() > worst.ordinal()) {
                worst = kind;
            }
        }
        sb.append("</ul>");
        switch (worst) {
            case OK:
                sb.insert(0, Messages.Publisher_ConnectionTest_Success(poweredBy));
                break;
            case WARNING:
                sb.insert(0, Messages.Publisher_ConnectionTest_Warning(poweredBy));
                break;
            case ERROR:
                sb.insert(0, Messages.Publisher_ConnectionTest_Error(poweredBy));
                break;
        }
        return FormValidation.respond(worst, String.format("<div class=\"%s\">%s</div>", worst.name().toLowerCase(Locale.ENGLISH), sb));
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
    public boolean configure(final StaplerRequest req, final JSONObject formData) throws Descriptor.FormException {
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
        return CredentialsProvider.lookupCredentials(StringCredentials.class, item, ACL.SYSTEM, List.of()).stream()
                .filter(c -> c.getId().equals(credentialId))
                .map(StringCredentials::getSecret)
                .map(Secret::getPlainText)
                .findFirst().orElse(StringUtils.EMPTY);
    }
}
