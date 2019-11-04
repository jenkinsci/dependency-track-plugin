[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/dependency-track-plugin/master)](https://ci.jenkins.io/job/Plugins/job/dependency-track-plugin)
[![License][license-image]][license-url]
[![Plugin Version](https://img.shields.io/jenkins/plugin/v/dependency-track.svg)](https://plugins.jenkins.io/dependency-track)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/dependency-track.svg?color=blue)](https://plugins.jenkins.io/dependency-track)
[![JIRA](https://img.shields.io/badge/issue_tracker-JIRA-red.svg)](https://issues.jenkins-ci.org/issues/?jql=component%20%3D%20dependency-track-plugin)
[![Website](https://img.shields.io/badge/https://-dependencytrack.org-blue.svg)](https://dependencytrack.org/)
[![Documentation](https://img.shields.io/badge/read-documentation-blue.svg)](https://docs.dependencytrack.org/)


# Dependency-Track Jenkins Plugin

Dependency-Track is an intelligent Software [Supply Chain Component Analysis] platform that allows organizations to 
identify and reduce risk from the use of third-party and open source components. 

![ecosystem overview](https://raw.githubusercontent.com/DependencyTrack/dependency-track/master/docs/images/integrations.png)

## Plugin Description 
The Dependency-Track Jenkins plugin aids in publishing [CycloneDX](https://cyclonedx.org/) and [SPDX](https://spdx.org/) 
Software Bill-of-Materials (SBOM) to the Dependency-Track platform.

Publishing SBOMs can be performed asynchronously or synchronously.

Asynchronous publishing simply uploads the SBOM to Dependency-Track and the job continues. Synchronous publishing waits for Dependency-Track to process the SBOM after being uploaded. Synchronous publishing has the benefit of displaying interactive job trends and per build findings.

![job trend](https://raw.githubusercontent.com/jenkinsci/dependency-track-plugin/master/docs/images/jenkins-job-trend.png)

![findings](https://raw.githubusercontent.com/jenkinsci/dependency-track-plugin/master/docs/images/jenkins-job-findings.png)

## Job Configuration
Once configured with a valid URL and API key, simply configure a job to publish the artifact.

<p><br></p>

![job configuration](https://raw.githubusercontent.com/jenkinsci/dependency-track-plugin/master/docs/images/jenkins-job-publish.png)

<p><br></p>

**Dependency-Track project:** Specifies the unique project ID to upload scan results to. This dropdown will be automatically populated with a list of projects.

**Artifact:** Specifies the file to upload. Paths are relative from the Jenkins workspace.

**Artifact Type:** Options are:
* Software Bill of Material (CycloneDX or SPDX)
* Dependency-Check Scan Result (XML)
* Synchronous mode: Uploads a SBOM to Dependency-Track and waits for Dependency-Track to process and return results. The results returned are identical to the auditable findings but exclude findings that have previously been suppressed. Analysis decisions and vulnerability details are included in the response. Synchronous mode is possible with Dependency-Track v3.3.1 and higher.

<p><br></p>

![risk thresholds](https://raw.githubusercontent.com/jenkinsci/dependency-track-plugin/master/docs/images/jenkins-job-thresholds.png)

<p><br></p>

When Synchronous mode is enabled, thresholds can be defined which can optionally put the job into an UNSTABLE or FAILURE state.

**Total Findings:** Sets the threshold for the total number of critical, high, medium, or low severity findings allowed. If the number of findings equals or is greater than the threshold for any one of the severities, the job status will be changed to UNSTABLE or FAILURE.

**New Findings:** Sets the threshold for the number of new critical, high, medium, or low severity findings allowed. If the number of new findings equals or is greater than the previous builds finding for any one of the severities, the job status will be changed to UNSTABLE or FAILURE.

## Global Configuration
To setup, navigate to Jenkins > System Configuration and complete the Dependency-Track section.

<p><br></p>

![global configuration](https://raw.githubusercontent.com/jenkinsci/dependency-track-plugin/master/docs/images/jenkins-global-odt.png)


Copyright & License
-------------------

Dependency-Track and the Dependency-Track Jenkins Plugin are Copyright (c) Steve Springett. All Rights Reserved.

Permission to modify and redistribute is granted under the terms of the Apache 2.0 license.

[Supply Chain Component Analysis]: https://www.owasp.org/index.php/Component_Analysis
[license-image]: https://img.shields.io/badge/license-apache%20v2-brightgreen.svg
[license-url]: https://github.com/jenkinsci/dependency-track-plugin/blob/master/LICENSE.txt
