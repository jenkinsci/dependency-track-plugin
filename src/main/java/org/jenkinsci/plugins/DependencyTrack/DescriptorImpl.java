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
import jakarta.annotation.Nullable;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.DependencyTrack.api.ApiClient;
import org.jenkinsci.plugins.DependencyTrack.api.ApiClientException;
import org.jenkinsci.plugins.DependencyTrack.model.Team;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
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
public class DescriptorImpl extends BuildStepDescriptor<Publisher> implements Serializable {

    private static final long serialVersionUID = -2018722914973282748L;

    private final transient ApiClientFactory clientFactory;

    /**
     * Specifies the base URL to Dependency-Track.
     */
    @Setter(onMethod_ = {@DataBoundSetter})
    private String dependencyTrackUrl;

    /**
     * Specifies the alternative base URL to the frontend of Dependency-Track.
     */
    @Setter(onMethod_ = {@DataBoundSetter})
    private String dependencyTrackFrontendUrl;

    /**
     * Specifies the credential-id for an API Key used for authentication.
     */
    @Getter(onMethod_ = {@Nullable})
    @Setter(onMethod_ = {@DataBoundSetter})
    private String dependencyTrackApiKey;

    /**
     * Specifies the credential-id for an OIDC token used for authentication via
     * workload identity federation.
     */
    @Getter(onMethod_ = {@Nullable})
    @Setter(onMethod_ = {@DataBoundSetter})
    private String dependencyTrackOidcToken;

    /**
     * Specifies the name of the workload identity provider in Dependency-Track.
     */
    @Getter(onMethod_ = {@Nullable})
    @Setter(onMethod_ = {@DataBoundSetter})
    private String dependencyTrackWorkloadIdentityProvider;

    /**
     * Specifies the name of the service account in Dependency-Track.
     */
    @Getter(onMethod_ = {@Nullable})
    @Setter(onMethod_ = {@DataBoundSetter})
    private String dependencyTrackServiceAccount;

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
     * Retrieve the projects to populate the dropdown of existing projects.
     *
     * @param dependencyTrackUrl the base URL to Dependency-Track
     * @param dependencyTrackApiKey the API key to use for authentication
     * @param dependencyTrackOidcToken credential-Id of the OIDC token to use for
     * authentication via workload identity federation
     * @param dependencyTrackWorkloadIdentityProvider name of the workload
     * identity provider
     * @param dependencyTrackServiceAccount name of the service account
     * @param item used to lookup credentials in job config. ignored in global
     * @return ListBoxModel of project names and their id
     */
    @POST
    public ListBoxModel doFillProjectIdItems(@QueryParameter final String dependencyTrackUrl, @QueryParameter final String dependencyTrackApiKey, @QueryParameter final String dependencyTrackOidcToken, @QueryParameter final String dependencyTrackWorkloadIdentityProvider, @QueryParameter final String dependencyTrackServiceAccount, @AncestorInPath @Nullable final Item item) {
        if (item == null) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        } else {
            item.checkPermission(Item.CONFIGURE);
        }
        final ListBoxModel projects = new ListBoxModel();
        try {
            // url may come from instance-config. if empty, then take it from global config (this)
            final String url = Optional.ofNullable(PluginUtil.parseBaseUrl(dependencyTrackUrl)).orElseGet(this::getDependencyTrackUrl);
            final Authentication auth = resolveAuthentication(dependencyTrackApiKey, dependencyTrackOidcToken, dependencyTrackWorkloadIdentityProvider, dependencyTrackServiceAccount, item);
            final ApiClient apiClient = createClient(url, auth);
            final List<ListBoxModel.Option> options = apiClient.getProjects().stream()
                    .map(p -> new ListBoxModel.Option(p.getName().concat(" ").concat(Optional.ofNullable(p.getVersion()).orElse("")).trim(), p.getUuid()))
                    .sorted(Comparator.comparing(o -> o.name))
                    .toList();
            projects.add(new ListBoxModel.Option(Messages.Publisher_ProjectList_Placeholder(), ""));
            projects.addAll(options);
        } catch (ApiClientException e) {
            projects.add(Messages.Builder_Error_Projects(e.getLocalizedMessage()), "");
        }
        return projects;
    }

    /**
     * Retrieve the credentials to populate the dropdown of possible api keys.
     *
     * @param credentialsId the current credential value of secret
     * @param item used to lookup credentials in job config. ignored in global
     * @return ListBoxModel of credentials
     */
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
                .includeAs(ACL.SYSTEM2, item, StringCredentials.class, List.of())
                .includeCurrentValue(credentialsId);
    }

    /**
     * Retrieve the credentials to populate the dropdown of possible OIDC
     * tokens.
     *
     * @param credentialsId the current credential value of secret
     * @param item used to lookup credentials in job config. ignored in global
     * @return ListBoxModel of credentials
     */
    @POST
    public ListBoxModel doFillDependencyTrackOidcTokenItems(@QueryParameter final String credentialsId, @AncestorInPath final Item item) {
        return doFillDependencyTrackApiKeyItems(credentialsId, item);
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

    /**
     * performs the connection including permission check for the global
     * configuration
     *
     * @param dependencyTrackUrl value of the URL as specified in the global
     * config
     * @param dependencyTrackApiKey credential-Id of the api key as specified in
     * the global config
     * @param dependencyTrackOidcToken credential-Id of the OIDC token as
     * specified in the global config
     * @param dependencyTrackWorkloadIdentityProvider name of the workload
     * identity provider as specified in the global config
     * @param dependencyTrackServiceAccount name of the service account as
     * specified in the global config
     * @param item used to check jenkins permissions
     * @return
     */
    @POST
    public FormValidation doTestConnectionGlobal(@QueryParameter final String dependencyTrackUrl, @QueryParameter final String dependencyTrackApiKey, @QueryParameter final String dependencyTrackOidcToken, @QueryParameter final String dependencyTrackWorkloadIdentityProvider, @QueryParameter final String dependencyTrackServiceAccount, @AncestorInPath @Nullable Item item) {
        return testConnection(dependencyTrackUrl, dependencyTrackApiKey, dependencyTrackOidcToken, dependencyTrackWorkloadIdentityProvider, dependencyTrackServiceAccount, false, false, item);
    }

    /**
     * performs the connection including permission check for the job
     * configuration
     *
     * @param dependencyTrackUrl value of the URL as specified in the job config
     * @param dependencyTrackApiKey credential-Id of the api key as specified in
     * the job config
     * @param dependencyTrackOidcToken credential-Id of the OIDC token as
     * specified in the job config
     * @param dependencyTrackWorkloadIdentityProvider name of the workload
     * identity provider as specified in the job config
     * @param dependencyTrackServiceAccount name of the service account as
     * specified in the job config
     * @param synchronous value for synchronous publishing as specified in the
     * job config
     * @param projectProperties value for setting project properties as
     * specified in the job config
     * @param item used to check jenkins permissions
     * @return
     */
    @POST
    public FormValidation doTestConnectionJob(@QueryParameter final String dependencyTrackUrl, @QueryParameter final String dependencyTrackApiKey, @QueryParameter final String dependencyTrackOidcToken, @QueryParameter final String dependencyTrackWorkloadIdentityProvider, @QueryParameter final String dependencyTrackServiceAccount, @QueryParameter final boolean synchronous, @QueryParameter final boolean projectProperties, @AncestorInPath @Nullable Item item) {
        return testConnection(dependencyTrackUrl, dependencyTrackApiKey, dependencyTrackOidcToken, dependencyTrackWorkloadIdentityProvider, dependencyTrackServiceAccount, synchronous, projectProperties, item);
    }

    /**
     * Performs an on-the-fly check of the Dependency-Track URL and api key
     * parameters by making a simple call to the server and validating the
     * response code.
     *
     * @param dependencyTrackUrl the base URL to Dependency-Track
     * @param dependencyTrackApiKey the credential-id of the API key to use for
     * authentication
     * @param dependencyTrackOidcToken the credential-id of the OIDC token to
     * use for authentication via workload identity federation
     * @param dependencyTrackWorkloadIdentityProvider name of the workload
     * identity provider
     * @param dependencyTrackServiceAccount name of the service account
     * @param synchronous if sync-mode is enabled or not
     * @param item used to check permission and lookup credentials
     * @return FormValidation
     */
    private FormValidation testConnection(final String dependencyTrackUrl, final String dependencyTrackApiKey, final String dependencyTrackOidcToken, final String dependencyTrackWorkloadIdentityProvider, final String dependencyTrackServiceAccount, final boolean synchronous, final boolean updateProjectProperties, @AncestorInPath @Nullable Item item) {
        if (item == null) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        } else {
            item.checkPermission(Item.CONFIGURE);
        }
        // url may come from instance-config. if empty, then take it from global config (this)
        final String url = Optional.ofNullable(PluginUtil.parseBaseUrl(dependencyTrackUrl)).orElseGet(this::getDependencyTrackUrl);
        final Authentication auth = resolveAuthentication(dependencyTrackApiKey, dependencyTrackOidcToken, dependencyTrackWorkloadIdentityProvider, dependencyTrackServiceAccount, item);
        if (doCheckDependencyTrackUrl(url, item).kind == FormValidation.Kind.OK && auth.isComplete()) {
            try {
                final ApiClient apiClient = createClient(url, auth);
                final var poweredBy = apiClient.testConnection();
                if (!poweredBy.startsWith("Dependency-Track v")) {
                    return FormValidation.error(Messages.Publisher_ConnectionTest_Error(poweredBy));
                }
                final var actualVersion = new VersionNumber(apiClient.getVersion());
                final var requiredVersion = new VersionNumber("4.12.0");
                if (actualVersion.isOlderThan(requiredVersion)) {
                    return FormValidation.error(Messages.Publisher_ConnectionTest_VersionWarning(actualVersion, requiredVersion));
                }
                if (auth.workloadIdentity()) {
                    // the permissions of a service account cannot be checked
                    return FormValidation.ok(Messages.Publisher_ConnectionTest_SuccessServiceAccount(poweredBy));
                }
                return checkTeamPermissions(apiClient, poweredBy, synchronous, updateProjectProperties);
            } catch (ApiClientException e) {
                return FormValidation.error(e, Messages.Publisher_ConnectionTest_Error(e.getMessage()));
            }
        }
        return FormValidation.error(Messages.Publisher_ConnectionTest_InputError());
    }

    private FormValidation checkTeamPermissions(final ApiClient apiClient, final String poweredBy, final boolean synchronous, final boolean projectProperties) throws ApiClientException {
        final Set<String> requiredPermissions = PluginUtil.buildRequiredPermissions(synchronous, projectProperties);
        final Set<String> optionalPermissions = PluginUtil.buildOptionalPermissions(synchronous, projectProperties);

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
            case OK -> sb.insert(0, Messages.Publisher_ConnectionTest_Success(poweredBy));
            case WARNING -> sb.insert(0, Messages.Publisher_ConnectionTest_Warning(poweredBy));
            case ERROR -> sb.insert(0, Messages.Publisher_ConnectionTest_Error(poweredBy));
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
    public boolean configure(final StaplerRequest2 req, final JSONObject formData) throws Descriptor.FormException {
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
    @Nullable
    public String getDependencyTrackUrl() {
        return PluginUtil.parseBaseUrl(dependencyTrackUrl);
    }

    /**
     * @return global configuration for dependencyTrackFrontendUrl
     */
    @Nullable
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

    private ApiClient createClient(final String baseUrl, final Authentication auth) throws ApiClientException {
        final int connectionTimeout = Math.max(dependencyTrackConnectionTimeout, 0);
        final int readTimeout = Math.max(dependencyTrackReadTimeout, 0);
        final var httpClient = PluginUtil.newHttpClient(connectionTimeout, readTimeout);
        final ApiClient apiClient = clientFactory.create(baseUrl, auth.workloadIdentity() ? "" : auth.secret(), new ConsoleLogger(), httpClient);
        if (auth.workloadIdentity()) {
            apiClient.exchangeWorkloadIdentityToken(auth.secret(), auth.provider(), auth.serviceAccount());
        }
        return apiClient;
    }

    /**
     * resolves the authentication to use. values may come from instance-config.
     * if empty, then take them from global config (this). an api key from
     * instance-config takes precedence over an OIDC token from global config.
     */
    @NonNull
    private Authentication resolveAuthentication(final String apiKeyId, final String oidcTokenId, final String provider, final String serviceAccount, final Item item) {
        final String instanceOidcTokenId = PluginUtil.trimToNull(oidcTokenId);
        final String effectiveOidcTokenId = instanceOidcTokenId != null || PluginUtil.trimToNull(apiKeyId) != null
                ? instanceOidcTokenId
                : PluginUtil.trimToNull(getDependencyTrackOidcToken());
        if (effectiveOidcTokenId != null) {
            return new Authentication(true, lookupApiKey(effectiveOidcTokenId, item),
                    Optional.ofNullable(PluginUtil.trimToNull(provider)).or(() -> Optional.ofNullable(getDependencyTrackWorkloadIdentityProvider())).orElse(""),
                    Optional.ofNullable(PluginUtil.trimToNull(serviceAccount)).or(() -> Optional.ofNullable(getDependencyTrackServiceAccount())).orElse(""));
        }
        return new Authentication(false, lookupApiKey(Optional.ofNullable(PluginUtil.trimToNull(apiKeyId)).orElseGet(this::getDependencyTrackApiKey), item), "", "");
    }

    @NonNull
    private String lookupApiKey(final String credentialId, final Item item) {
        return CredentialsProvider.lookupCredentialsInItem(StringCredentials.class, item, ACL.SYSTEM2, List.of()).stream()
                .filter(c -> c.getId().equals(credentialId))
                .map(StringCredentials::getSecret)
                .map(Secret::getPlainText)
                .findFirst().orElse("");
    }

    /**
     * @param workloadIdentity whether workload identity federation is used
     * @param secret the api key or the OIDC token
     * @param provider name of the workload identity provider
     * @param serviceAccount name of the service account
     */
    private record Authentication(boolean workloadIdentity, @NonNull String secret, @NonNull String provider, @NonNull String serviceAccount) {

        boolean isComplete() {
            return !secret.isBlank() && (!workloadIdentity || !provider.isBlank() && !serviceAccount.isBlank());
        }
    }
}
