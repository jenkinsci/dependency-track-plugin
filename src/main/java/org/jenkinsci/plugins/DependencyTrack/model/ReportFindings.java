package org.jenkinsci.plugins.DependencyTrack.model;

import lombok.Value;

@Value
public class ReportFindings {
	
	private String componentName;
	private String componentGroup;
	private String componentVersion;
	private String vulnerabilityID;
	private Integer severity;
	private String cwe;
	private boolean isSuppressed;
	
}