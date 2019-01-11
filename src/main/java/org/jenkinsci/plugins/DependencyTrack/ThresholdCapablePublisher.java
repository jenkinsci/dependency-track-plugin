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

public abstract class ThresholdCapablePublisher extends Recorder implements Serializable {

    private static final long serialVersionUID = 8844465732219790336L;

    private final Thresholds thresholds = new Thresholds();

    Thresholds getThresholds() {
        return thresholds;
    }

    @DataBoundSetter
    public void setTotalThresholdAll(final int totalThresholdAll) {
        getThresholds().totalFindings.all = totalThresholdAll;
    }

    @DataBoundSetter
    public void settotalThresholdCritical(final int totalThresholdCritical) {
        getThresholds().totalFindings.critical = totalThresholdCritical;
    }

    @DataBoundSetter
    public void setTotalThresholdHigh(final int totalThresholdHigh) {
        getThresholds().totalFindings.high = totalThresholdHigh;
    }

    @DataBoundSetter
    public void setTotalThresholdMedium(final int totalThresholdMedium) {
        getThresholds().totalFindings.medium = totalThresholdMedium;
    }

    @DataBoundSetter
    public void setTotalThresholdLow(final int totalThresholdLow) {
        getThresholds().totalFindings.low = totalThresholdLow;
    }

    @DataBoundSetter
    public void setTotalThresholdAnalysisExploitable(final boolean totalThresholdAnalysisExploitable) {
        getThresholds().totalFindings.limitToAnalysisExploitable = totalThresholdAnalysisExploitable;
    }

    @DataBoundSetter
    public void setNewThresholdAll(final int newThresholdAll) {
        getThresholds().newFindings.all = newThresholdAll;
    }

    @DataBoundSetter
    public void setNewThresholdCritical(final int newThresholdCritical) {
        getThresholds().newFindings.critical = newThresholdCritical;
    }

    @DataBoundSetter
    public void setNewThresholdHigh(final int newThresholdHigh) {
        getThresholds().newFindings.high = newThresholdHigh;
    }

    @DataBoundSetter
    public void setNewThresholdMedium(final int newThresholdMedium) {
        getThresholds().newFindings.medium = newThresholdMedium;
    }

    @DataBoundSetter
    public void setNewThresholdLow(final int newThresholdLow) {
        getThresholds().newFindings.low = newThresholdLow;
    }

    @DataBoundSetter
    public void setNewThresholdAnalysisExploitable(final boolean newThresholdAnalysisExploitable) {
        getThresholds().newFindings.limitToAnalysisExploitable = newThresholdAnalysisExploitable;
    }
}
