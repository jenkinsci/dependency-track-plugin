package org.jenkinsci.plugins.DependencyTrack.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Project implements Serializable {

    private static final long serialVersionUID = 5615023685011011641L;
	
    String name;
    String description;
    String version;
    String uuid;
    Collection<String> tags;
    LocalDateTime lastBomImport;
    String lastBomImportFormat;
    Double lastInheritedRiskScore;
    Boolean active;
    String swidTagId;
    String group;
}
