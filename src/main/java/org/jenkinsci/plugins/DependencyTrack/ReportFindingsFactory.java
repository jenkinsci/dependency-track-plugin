package org.jenkinsci.plugins.DependencyTrack;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.ReportFindings;

public class ReportFindingsFactory {

	private ReportFindingsFactory() {
		throw new UnsupportedOperationException("Should not instantiate, this is a helper class");
	}

	public static List<ReportFindings> getSortedReportFindings(List<Finding> findings) {
		Map<Integer, ReportFindings> m = findings.stream()
			.map(f -> ReportFindings.builder()
					.componentName(f.getComponent().getName())
					.componentGroup(f.getComponent().getGroup())
					.componentVersion(f.getComponent().getVersion())
					.vulnerabilityID(f.getVulnerability().getSource() + ":" + f.getVulnerability().getVulnId())
					.severity(f.getVulnerability().getSeverity().name())
					.cwe(f.getVulnerability().getCweId() + " " + f.getVulnerability().getCweName())
					.isSuppressed(f.getAnalysis().isSuppressed())
					.severityRank(f.getVulnerability().getSeverityRank()).build())
			.collect(Collectors.toList()).stream() // stream of ReportFindings
			.collect(Collectors.toMap(ReportFindings::getSeverityRank, Function.identity(), (o1, o2) -> o1, TreeMap::new));
		return m.keySet().stream().map(k -> m.get(k)).collect(Collectors.toList());
	}
}
