package org.jenkinsci.plugins.DependencyTrack.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

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

    public Project(String name, String description, String version, String uuid, List<String> tags, LocalDateTime lastBomImport,
                   String lastBomImportFormat, Double lastInheritedRiskScore, Boolean active) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.uuid = uuid;
        this.tags = tags;
        this.lastBomImport = lastBomImport;
        this.lastBomImportFormat = lastBomImportFormat;
        this.lastInheritedRiskScore = lastInheritedRiskScore;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public String getUuid() {
        return uuid;
    }

    public List<String> getTags() {
        return tags;
    }

    public LocalDateTime getLastBomImport() {
        return lastBomImport;
    }

    public String getLastBomImportFormat() {
        return lastBomImportFormat;
    }

    public Double getLastInheritedRiskScore() {
        return lastInheritedRiskScore;
    }

    public Boolean isActive() {
        return active;
    }
}
