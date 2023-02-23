package org.jenkinsci.plugins.DependencyTrack.model;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReportFindings {
	
	private String componentName;
	private String componentGroup;
	private String componentVersion;
	private String vulnerabilityID;
	private String severity;
	private String cwe;
	private Boolean isSuppressed;
	public static class ReportFindingsBuilder {
		public ReportFindingsBuilder severity(Integer aSeverity) {
			List<Severity> severities=Stream.of(Severity.values()).filter(s -> s.ordinal() == aSeverity).collect(Collectors.toList());
			if (severities.size()==1) {
				this.severity=severities.get(0).name();
				return this;
			} else {
				throw new IllegalArgumentException("Illegal value for severity in report findings: "+aSeverity.toString());
			}
		}
	}
	
}