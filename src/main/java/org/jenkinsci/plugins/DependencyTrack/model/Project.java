package org.jenkinsci.plugins.DependencyTrack.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Project implements Serializable {

    private static final long serialVersionUID = 5615023685011011641L;
	
    private String name;
    private String description;
    private String version;
    private String uuid;
    private List<String> tags;
    private LocalDateTime lastBomImport;
    private String lastBomImportFormat;
    private Double lastInheritedRiskScore;
    private Boolean active;
}
