package org.jenkinsci.plugins.DependencyTrack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.SeverityDistribution;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;

public class JasperReports {
	
	public enum REPORT_FORMAT {
		pdf, xlsx;
	}
	
	public static final String PDF_REPORT_FILENAME="dt_summary."+REPORT_FORMAT.pdf.name();
	public static final String XLSX_REPORT_FILENAME="dt_summary."+REPORT_FORMAT.xlsx.name();
	
	private JasperReports() {
		throw new UnsupportedOperationException("Instantiation of helper class not supported");
	}
	static protected byte[] createSummaryReport2(String aReportSubTitle, SeverityDistribution severityDistribution, List<Finding> findings, REPORT_FORMAT format) throws JRException  {
		JasperReport jr=JasperCompileManager.compileReport("report/Dependency_Track_Summary.jrxml");
		final Map<String, Object> reportParameters=new HashMap<String, Object>();
		reportParameters.put("REPORT_TITLE", "Dependency-Track");
		reportParameters.put("REPORT_SUBTITLE", aReportSubTitle);
		reportParameters.put("SEVERITY_DISTRIBUTION", severityDistribution.toString());
		JasperPrint jp=JasperFillManager.fillReport(jr, reportParameters, new JRBeanCollectionDataSource(ReportFindingsFactory.getSortedReportFindings(findings)));
		byte[] jasperBytes=null;
		switch (format) {
		case pdf:
			jasperBytes=JasperExportManager.exportReportToPdf(jp);
			break;

		case xlsx:
			JRXlsxExporter exporter = new JRXlsxExporter();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			exporter.setExporterInput(new SimpleExporterInput(jp));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(baos));
			SimpleXlsxReportConfiguration config = new SimpleXlsxReportConfiguration();
			config.setOnePagePerSheet(true);
			config.setDetectCellType(true);
			config.setCollapseRowSpan(false);
			config.setRemoveEmptySpaceBetweenRows(true);
			config.setRemoveEmptySpaceBetweenColumns(true);
			
			exporter.setConfiguration(config);
			exporter.exportReport();
			jasperBytes=baos.toByteArray();
			
			break;

		}
		return jasperBytes;
	}
	static protected byte[] createSummaryReport(String aReportSubTitle, SeverityDistribution severityDistribution, List<Finding> findings, REPORT_FORMAT format) throws IOException  {
		try {
			return createSummaryReport2(aReportSubTitle, severityDistribution, findings, format);
		} catch (JRException e) {
			throw new IOException(e);
		}
	}
	

}
