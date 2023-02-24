package org.jenkinsci.plugins.DependencyTrack;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.assertj.core.util.Files;
import org.jenkinsci.plugins.DependencyTrack.JasperReports.REPORT_FORMAT;
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.SeverityDistribution;
import org.jenkinsci.plugins.DependencyTrack.model.Vulnerability;
import org.junit.Before;
import org.junit.Test;

import net.sf.jasperreports.engine.JRException;

public class JasperReportsTest {

    private ResultAction uut;
    private static final String subTitle="mfjsync: Vulnerability Report";
	
	private List<Finding> getTestFindings() {
        File findings = new File("src/test/resources/findings.json");
        return FindingParser.parse(Files.contentOf(findings, StandardCharsets.UTF_8));
    }
    
    @Before
    public void setup() {
    	uut=createResultAction();
    }

    @Test
    public void testCreateSummaryPdfReport() throws IOException, JRException {
        byte[] result = JasperReports.createSummaryReport(subTitle, uut.getSeverityDistribution(), uut.getFindings(), REPORT_FORMAT.pdf);
        File pdfFile=new File("target/Dependency_Track_Summary.pdf");
        try (FileOutputStream os = new FileOutputStream(pdfFile)) {
        	os.write(result);
        }
        String resultPrefix="%PDF-1.5";
        assertThat(result).withFailMessage("Excel document does not start with %s", resultPrefix).startsWith(resultPrefix.getBytes());
    }

    @Test
    public void testCreateSummaryCsvReport() throws IOException, JRException {
        byte[] result = JasperReports.createSummaryReport(subTitle, uut.getSeverityDistribution(), uut.getFindings(), REPORT_FORMAT.xlsx);
        File pdfFile=new File("target/Dependency_Track_Summary.xlsx");
        try (FileOutputStream os = new FileOutputStream(pdfFile)) {
        	os.write(result);
        }
        String resultPrefix="PK";
        assertThat(result).withFailMessage("Excel document does not start with %s", resultPrefix).startsWith(resultPrefix.getBytes());
    }

	private ResultAction createResultAction() {
		final SeverityDistribution severityDistribution=new SeverityDistribution(1);
    	List<Finding> findings = getTestFindings();
        findings.stream().map(Finding::getVulnerability).map(Vulnerability::getSeverity).forEach(severityDistribution::add);

        final ResultAction uut = new ResultAction(getTestFindings(), severityDistribution);
		return uut;
	}

}
