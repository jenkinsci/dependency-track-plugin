package org.jenkinsci.plugins.DependencyTrack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jenkinsci.plugins.DependencyTrack.model.Analysis;
import org.jenkinsci.plugins.DependencyTrack.model.Component;
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.ReportFindings;
import org.jenkinsci.plugins.DependencyTrack.model.Severity;
import org.jenkinsci.plugins.DependencyTrack.model.Vulnerability;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

public class ReportFindingsTestHarness {
	static String apikey="-apikey-";
    
    static public List<ReportFindings> getSortedReportFindings() {
    	return ReportFindingsFactory.getSortedReportFindings(new ArrayList<Finding>(Arrays.asList(
            	new Finding(new Component("cuuid1", "cname", "cgroup", "cversion", "purl1"),
                		new Vulnerability("vuuid1", "source", "vulnid1", "title", "subtitle", "description", "recommendation", Severity.LOW, Severity.LOW.ordinal(), 432, "cweName"),
                		new Analysis("state", false),
                		apikey),
                	new Finding(new Component("cuuid2", "cname2", "cgroup2", "cversion2", "purl2"),
                    		new Vulnerability("vuuid2", "source2", "vulnid2", "title2", "subtitle2", "description2", "recommendation2", Severity.CRITICAL, Severity.CRITICAL.ordinal(), 433, "cweName2"),
                    		new Analysis("state", false),
                    		apikey)
               )));
    }
    public static void main(String[] args) {
		System.out.println(getSortedReportFindings());
	}
    
    static public JRDataSource getDS() {
    	return new JRBeanCollectionDataSource(getSortedReportFindings());
    }

}