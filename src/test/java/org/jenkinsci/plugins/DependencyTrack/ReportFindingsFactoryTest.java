package org.jenkinsci.plugins.DependencyTrack;

import static org.junit.Assert.fail;

import org.jenkinsci.plugins.DependencyTrack.model.ReportFindings;
import org.junit.Test;

public class ReportFindingsFactoryTest {

	@Test
	public void testGetReportFindings() {
		int lastSeverityRank=-1;
		for (ReportFindings reportFindings : ReportFindingsTestHarness.getSortedReportFindings()) {
			int currentRank=reportFindings.getSeverityRank();
			if (currentRank<lastSeverityRank) fail("The report findinds returned by the factory are not sorted");
			lastSeverityRank=currentRank;
		}
	}

}
