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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;

@Data
@Setter(onMethod_ = {@DataBoundSetter})
@EqualsAndHashCode(callSuper = false)
public abstract class ThresholdCapablePublisher extends Recorder implements Serializable {

    private static final long serialVersionUID = 8844465732219790336L;

    private Integer unstableTotalCritical;
    private Integer unstableTotalHigh;
    private Integer unstableTotalMedium;
    private Integer unstableTotalLow;
    private Integer failedTotalCritical;
    private Integer failedTotalHigh;
    private Integer failedTotalMedium;
    private Integer failedTotalLow;
    private boolean totalThresholdAnalysisExploitable;

    private Integer unstableNewCritical;
    private Integer unstableNewHigh;
    private Integer unstableNewMedium;
    private Integer unstableNewLow;
    private Integer failedNewCritical;
    private Integer failedNewHigh;
    private Integer failedNewMedium;
    private Integer failedNewLow;
    private boolean newThresholdAnalysisExploitable;

    Thresholds getThresholds() {
        final Thresholds thresholds = new Thresholds();
        thresholds.totalFindings.unstableCritical = unstableTotalCritical;
        thresholds.totalFindings.unstableHigh = unstableTotalHigh;
        thresholds.totalFindings.unstableMedium = unstableTotalMedium;
        thresholds.totalFindings.unstableLow = unstableTotalLow;
        thresholds.totalFindings.failedCritical = failedTotalCritical;
        thresholds.totalFindings.failedHigh = failedTotalHigh;
        thresholds.totalFindings.failedMedium = failedTotalMedium;
        thresholds.totalFindings.failedLow = failedTotalLow;
        thresholds.totalFindings.limitToAnalysisExploitable = totalThresholdAnalysisExploitable;

        thresholds.newFindings.unstableCritical = unstableNewCritical;
        thresholds.newFindings.unstableHigh = unstableNewHigh;
        thresholds.newFindings.unstableMedium = unstableNewMedium;
        thresholds.newFindings.unstableLow = unstableNewLow;
        thresholds.newFindings.failedCritical = failedNewCritical;
        thresholds.newFindings.failedHigh = failedNewHigh;
        thresholds.newFindings.failedMedium = failedNewMedium;
        thresholds.newFindings.failedLow = failedNewLow;
        thresholds.newFindings.limitToAnalysisExploitable = newThresholdAnalysisExploitable;
        return thresholds;
    }

}
