# Dependency-Track Jenkins Plugin - Changelog

## Unreleased
### ⚠ Breaking
### ⭐ New Features
### 🐞 Bugs Fixed
- avoid "JSON Payload Too Large" error ([#313](https://github.com/jenkinsci/dependency-track-plugin/issues/313))

## v6.0.0 - 2025-01-18
### ⚠ Breaking
- require Jenkins 2.479.1 or newer
- require Java 17 or newer (required since Jenkins 2.479.1)
- require Dependency-Track 4.12 or newer ([#286](https://github.com/jenkinsci/dependency-track-plugin/issues/286))

### ⭐ New Features
- Support "isLatest" flag ([#286](https://github.com/jenkinsci/dependency-track-plugin/issues/286))

### 🐞 Bugs Fixed

## v5.2.0 - 2024-12-08
### ⚠ Breaking
### ⭐ New Features
- Support variables in project properties ([JENKINS-74822](https://issues.jenkins.io/browse/JENKINS-74822))

### 🐞 Bugs Fixed

## v5.1.0 - 2024-09-20
### ⚠ Breaking
### ⭐ New Features
- Support for specifying the parent project using its name and version as an alternative to its ID ([#261](https://github.com/jenkinsci/dependency-track-plugin/issues/261))
- Include artifact name in Publishing Logline ([#264](https://github.com/jenkinsci/dependency-track-plugin/issues/264))
- Support for Policy Violations ([#130](https://github.com/jenkinsci/dependency-track-plugin/issues/130))

### 🐞 Bugs Fixed

## v5.0.0 - 2024-05-30
### ⚠ Breaking
- require Jenkins 2.440.1 or newer
- require Java 11 or newer ([required since Jenkins 2.361.1](https://www.jenkins.io/blog/2022/06/28/require-java-11/))
- require Dependency-Track 4.9 or newer
- New findings are only evaluated from the second build onwards ([#113](https://github.com/jenkinsci/dependency-track-plugin/issues/113))

### ⭐ New Features
- Allow `overrideGlobals` to override global timeout and interval settings ([#182](https://github.com/jenkinsci/dependency-track-plugin/issues/182))
- Use the proxy that is configured in Jenkins ([#181](https://github.com/jenkinsci/dependency-track-plugin/issues/181))
- Support threshold for unassigned findings ([#158](https://github.com/jenkinsci/dependency-track-plugin/issues/158))
- Supports HTTP/2
- In the event of an unexpected exception, each call to Dependency-Track is retried within an uniformly distributed, randomly generated period in the range of 50-500ms.
- A warning is emitted when threshold values are configured but synchronous mode is disabled.
Add Support for Identification of Aliases ... by ignoring them ([#168](https://github.com/jenkinsci/dependency-track-plugin/issues/168))

### 🐞 Bugs Fixed
- The settings for the threshold values are now only visible when synchronous mode is enabled. This will hopefully avoid misunderstandings/misconfigurations.

## v4.3.1 - 2023-04-12
### ⚠ Breaking
### ⭐ New Features
### 🐞 Bugs Fixed
- Remove usages of `l:css` ([#160](https://github.com/jenkinsci/dependency-track-plugin/issues/160))

## v4.3.0 - 2022-02-20
### ⚠ Breaking
### ⭐ New Features
- Added support for parent-child-relationships of projects with Dependency-Track v4.7 and newer (fixes [#139](https://github.com/jenkinsci/dependency-track-plugin/issues/139))

### 🐞 Bugs Fixed
- Searching on the result page was partially broken due to [a bug in bootstrap-vue 2.22+](https://github.com/bootstrap-vue/bootstrap-vue/issues/6967)

## v4.2.0 - 2022-07-04
### ⚠ Breaking
### ⭐ New Features
- The connection test will also check server-side permissions for Dependency-Track v4.4 and newer (fixes [#13](https://github.com/jenkinsci/dependency-track-plugin/issues/13))

### 🐞 Bugs Fixed
- classic jobs with sync mode and no project ID used the looked-up ID in future runs, although they should not (fixes [#98](https://github.com/jenkinsci/dependency-track-plugin/issues/98))
- When using "New Findings" thresholds, the plugin is now looking for the latest succesful build with a report instead of just the previous build with the report.

## v4.1.1 - 2022-03-06
### ⚠ Breaking
### ⭐ New Features
### 🐞 Bugs Fixed
- The options "Dependency-Track project name" and "Dependency-Track project version" were only visible after saving and reloading the configuration page, although the global configuration "Auto Create projects" was set.
- Fixed an issue with "Dependency-Track project" in classic (freestyle) jobs and Jenkins 2.319 LTS that caused the value to be "null" instead of empty, resulting in upload errors. Affected users should edit and save the job after updating to this plugin version.

## v4.1.0 - 2022-02-28
### ⚠ Breaking
### ⭐ New Features
- allow to specify tags that should be set for the project (fixes [#12](https://github.com/jenkinsci/dependency-track-plugin/issues/12))
- allow to specify SWID and group that should be set for the project (fixes [#50](https://github.com/jenkinsci/dependency-track-plugin/issues/50))
- allow to specify a description that should be set for the project

### 🐞 Bugs Fixed
- Analysis result information not shown when CSRF Protection is turned off (fixes [#73](https://github.com/jenkinsci/dependency-track-plugin/issues/73))
- The threshold for new findings used the last build, even though it may not have had a Dependency-Track analysis result.

## v4.0.0 - 2021-08-29
### ⚠ Breaking
- minimum required Jenkins version is now 2.289.2

### ⭐ New Features
- replaced inline JavaScript ... one step closer to compatibility with the CSP header
- add 'min' values in field definitions of forms
- uses modern div-layout for threshold level settings section
- Clicking on the x-axis label (the job number) of the trend graph will take you directly to the full report.
- added german translation
- display report summary on build run page containing the number of severities found

### 🐞 Bugs Fixed
- enforce Job/read permission in order to read the analysis results for a build run and the trend data on the project page
- configured threshold levels in classic jobs where empty in the UI after saving them and reloading the config page. saving them again resulted in the deletion of previous none-empty values.

## v3.1.1 - 2021-03-30
### 🐞 Bugs Fixed
- [SECURITY-2250](https://issues.jenkins.io/browse/SECURITY-2250). Thanks to Justin Philip for reporting this issue.

## v3.1.0 - 2021-02-08
### ⭐ New Features
- allow to specify an alternative URL for the Dependency-Track Frontend (fixes [#22](https://github.com/jenkinsci/dependency-track-plugin/issues/22))
- added links to npm security advisories

### 🐞 Bugs Fixed
- verify that `projectId` is set when auto create projects is not enabled

## v3.0.2 - 2020-12-09
### 🐞 Bugs Fixed
- link to project page not working for Dependency-Track older than v3.8, part 2 (previous fix was incomplete)

## v3.0.1 - 2020-12-08
### 🐞 Bugs Fixed
- link to project page not working for Dependency-Track older than v3.8 (fixes [#17](https://github.com/jenkinsci/dependency-track-plugin/issues/17))
- Report does not render correctly in Firefox (fixes [#18](https://github.com/jenkinsci/dependency-track-plugin/issues/18))

## v3.0.0 - 2020-11-30
### ⚠ Breaking
- Internet Explorer 11 is not supported anymore
- minimum required Jenkins version is now 2.249.2
- configuration is not compatible with previous versions
  - due to internal changes
  - **API key is now of type "Secret Text" credential**

### ⭐ New Features
- allow to override global values for Dependency-Track URL, API key and "Auto Create Projects" in job definition (fixes [JENKINS-55926](https://issues.jenkins.io/browse/JENKINS-55926))
- support multiple invocations in a build run (fixes [JENKINS-55926](https://issues.jenkins.io/browse/JENKINS-55926)).

  **please note**: Only the result of the last invocation using `synchronous=true` will contribute to the result report page and the history on the job page.

- allow to specify connect and response timeouts
- re-designed result report page
- added link on the sidebar to Dependency-Track project page for each build (*only for new builds*, fixes [JENKINS-55627](https://issues.jenkins.io/browse/JENKINS-55627))
- support dark theme of Jenkins 2.249
- [JENKINS-57697](https://issues.jenkins.io/browse/JENKINS-57697): more detailed error logging
- API key is now a "Secret Text" credential
- list of projects in job configuration page is now sorted and includes only active projects

### 🐞 Bugs Fixed
- unable to use `projectName` and `projectVersion` instead of `projectId` in classic job types
- sorting of column "Severity" in result report table was broken
- corrected the help information for global setting "Polling Timeout"

## v2.3.0 - 2020-05-21
### ⚠ Breaking
- removed `artifactType` parameter
- minimum required Jenkins version is now 2.222.3

### ⭐ New Features
-  [JENKINS-57640](https://issues.jenkins.io/browse/JENKINS-57640): Support Configuration As Code
