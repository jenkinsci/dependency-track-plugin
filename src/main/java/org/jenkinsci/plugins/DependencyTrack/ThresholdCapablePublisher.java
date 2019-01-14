/*
 * This file is part of Dependency-Track Jenkins plugin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.DependencyTrack;

import hudson.tasks.Recorder;
import org.jenkinsci.plugins.DependencyTrack.model.Thresholds;
import org.kohsuke.stapler.DataBoundSetter;
import java.io.Serializable;

@SuppressWarnings("unused")
public abstract class ThresholdCapablePublisher extends Recorder implements Serializable {

    private static final long serialVersionUID = 8844465732219790336L;

    private Integer totalThresholdAll;
    private Integer totalThresholdCritical;
    private Integer totalThresholdHigh;
    private Integer totalThresholdMedium;
    private Integer totalThresholdLow;
    private boolean totalThresholdAnalysisExploitable;
    private boolean totalThresholdFailBuild;

    private Integer newThresholdAll;
    private Integer newThresholdCritical;
    private Integer newThresholdHigh;
    private Integer newThresholdMedium;
    private Integer newThresholdLow;
    private boolean newThresholdAnalysisExploitable;
    private boolean newThresholdFailBuild;

    Thresholds getThresholds() {
        final Thresholds thresholds = new Thresholds();
        thresholds.totalFindings.critical = totalThresholdCritical;
        thresholds.totalFindings.high = totalThresholdHigh;
        thresholds.totalFindings.medium = totalThresholdMedium;
        thresholds.totalFindings.low = totalThresholdLow;
        thresholds.totalFindings.limitToAnalysisExploitable = totalThresholdAnalysisExploitable;
        thresholds.totalFindings.failBuild = totalThresholdFailBuild;

        thresholds.newFindings.critical = newThresholdCritical;
        thresholds.newFindings.high = newThresholdHigh;
        thresholds.newFindings.medium = newThresholdMedium;
        thresholds.newFindings.low = newThresholdLow;
        thresholds.newFindings.limitToAnalysisExploitable = newThresholdAnalysisExploitable;
        thresholds.newFindings.failBuild = newThresholdFailBuild;
        return thresholds;
    }

    public Integer getTotalThresholdAll() {
        return totalThresholdAll;
    }

    @DataBoundSetter
    public void setTotalThresholdAll(final Integer totalThresholdAll) {
        this.totalThresholdAll = totalThresholdAll;
    }

    public Integer getTotalThresholdCritical() {
        return totalThresholdCritical;
    }

    @DataBoundSetter
    public void setTotalThresholdCritical(final Integer totalThresholdCritical) {
        this.totalThresholdCritical = totalThresholdCritical;
    }

    public Integer getTotalThresholdHigh() {
        return totalThresholdHigh;
    }

    @DataBoundSetter
    public void setTotalThresholdHigh(final Integer totalThresholdHigh) {
        this.totalThresholdHigh = totalThresholdHigh;
    }

    public Integer getTotalThresholdMedium() {
        return totalThresholdMedium;
    }

    @DataBoundSetter
    public void setTotalThresholdMedium(final Integer totalThresholdMedium) {
        this.totalThresholdMedium = totalThresholdMedium;
    }

    public Integer getTotalThresholdLow() {
        return totalThresholdLow;
    }

    @DataBoundSetter
    public void setTotalThresholdLow(final Integer totalThresholdLow) {
        this.totalThresholdLow = totalThresholdLow;
    }

    public boolean getTotalThresholdAnalysisExploitable() {
        return totalThresholdAnalysisExploitable;
    }

    @DataBoundSetter
    public void setTotalThresholdAnalysisExploitable(final boolean totalThresholdAnalysisExploitable) {
        this.totalThresholdAnalysisExploitable = totalThresholdAnalysisExploitable;
    }

    public boolean getTotalThresholdFailBuild() {
        return totalThresholdFailBuild;
    }

    @DataBoundSetter
    public void setTotalThresholdFailBuild(boolean totalThresholdFailBuild) {
        this.totalThresholdFailBuild = totalThresholdFailBuild;
    }

    public Integer getNewThresholdAll() {
        return newThresholdAll;
    }

    @DataBoundSetter
    public void setNewThresholdAll(final Integer newThresholdAll) {
        this.newThresholdAll = newThresholdAll;
    }

    public Integer getNewThresholdCritical() {
        return newThresholdCritical;
    }

    @DataBoundSetter
    public void setNewThresholdCritical(final Integer newThresholdCritical) {
        this.newThresholdCritical = newThresholdCritical;
    }

    public Integer getNewThresholdHigh() {
        return newThresholdHigh;
    }

    @DataBoundSetter
    public void setNewThresholdHigh(final Integer newThresholdHigh) {
        this.newThresholdHigh = newThresholdHigh;
    }

    public Integer getNewThresholdMedium() {
        return newThresholdMedium;
    }

    @DataBoundSetter
    public void setNewThresholdMedium(final Integer newThresholdMedium) {
        this.newThresholdMedium = newThresholdMedium;
    }

    public Integer getNewThresholdLow() {
        return newThresholdLow;
    }

    @DataBoundSetter
    public void setNewThresholdLow(final Integer newThresholdLow) {
        this.newThresholdLow = newThresholdLow;
    }

    public boolean getNewThresholdAnalysisExploitable() {
        return newThresholdAnalysisExploitable;
    }

    @DataBoundSetter
    public void setNewThresholdAnalysisExploitable(final boolean newThresholdAnalysisExploitable) {
        this.newThresholdAnalysisExploitable = newThresholdAnalysisExploitable;
    }

    public boolean getNewThresholdFailBuild() {
        return newThresholdFailBuild;
    }

    @DataBoundSetter
    public void setNewThresholdFailBuild(boolean newThresholdFailBuild) {
        this.newThresholdFailBuild = newThresholdFailBuild;
    }
}
