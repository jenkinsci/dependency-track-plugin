package org.jenkinsci.plugins.DependencyTrack;

import java.util.List;
import java.util.stream.Collectors;

import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.ReportFindings;

public class ReportFindingsFactory {
	
	private ReportFindingsFactory() {
		throw new UnsupportedOperationException("Should not instantiate, this is a helper class");
	}
	public static List<ReportFindings> getReportFindings(List<Finding> findings) {
		return findings.stream().map(f -> 
			ReportFindings.builder().componentName(f.getComponent().getName())
				.componentGroup(f.getComponent().getGroup())
				.componentVersion(f.getComponent().getVersion())
				.vulnerabilityID(f.getVulnerability().getSource()+":"+f.getVulnerability().getVulnId())
				.severity(f.getVulnerability().getSeverityRank())
				.cwe(f.getVulnerability().getCweId()+" "+f.getVulnerability().getCweName())
				.isSuppressed(f.getAnalysis().isSuppressed()).build()
			).collect(Collectors.toList());
	}
}
