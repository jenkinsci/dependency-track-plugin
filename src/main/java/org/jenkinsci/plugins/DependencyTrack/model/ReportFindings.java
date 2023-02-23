package org.jenkinsci.plugins.DependencyTrack.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReportFindings implements Comparable<ReportFindings> {
	
	private String componentName;
	private String componentGroup;
	private String componentVersion;
	private String vulnerabilityID;
	private String severity;
	private String cwe;
	private Boolean isSuppressed;
	private Integer severityRank;
	@Override
	public int compareTo(ReportFindings o) {
		return this.severityRank.compareTo(o.getSeverityRank());
	}
	
}