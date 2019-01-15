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
package org.jenkinsci.plugins.DependencyTrack.model;

import hudson.model.Result;
import java.io.Serializable;
import java.util.List;

public class RiskGate implements Serializable {

    private static final long serialVersionUID = 171256230735670985L;

    private Thresholds thresholds;

    public RiskGate(Thresholds thresholds) {
        this.thresholds = thresholds;
    }

    /**
     * Evaluates if the current results meet or exceed the defined threshold.
     * @param previousDistribution
     * @param previousFindings
     * @param currentDistribution
     * @param currentFindings
     * @return a Result
     */
    public Result evaluate(final SeverityDistribution previousDistribution,
                                   final List<Finding> previousFindings,
                                   final SeverityDistribution currentDistribution,
                                   final List<Finding> currentFindings) {

        Result result = Result.SUCCESS;
        if (currentDistribution != null) {
            if ((thresholds.totalFindings.critical != null && currentDistribution.getCritical() > 0 && currentDistribution.getCritical() >= thresholds.totalFindings.critical)
                    || (thresholds.totalFindings.high != null && currentDistribution.getHigh() > 0 && currentDistribution.getHigh() >= thresholds.totalFindings.high)
                    || (thresholds.totalFindings.medium != null && currentDistribution.getMedium() > 0 && currentDistribution.getMedium() >= thresholds.totalFindings.medium)
                    || (thresholds.totalFindings.low != null && currentDistribution.getLow() > 0 && currentDistribution.getLow() >= thresholds.totalFindings.low)) {

                if (thresholds.totalFindings.failBuild) {
                    return Result.FAILURE;
                } else {
                    result = Result.UNSTABLE;
                }
            }
        }

        if (currentDistribution != null && previousDistribution != null) {
            if ((thresholds.newFindings.critical != null && currentDistribution.getCritical() > 0 && currentDistribution.getCritical() >= previousDistribution.getCritical() + thresholds.newFindings.critical)
                    || (thresholds.newFindings.high != null && currentDistribution.getHigh() > 0 && currentDistribution.getHigh() >= previousDistribution.getHigh() + thresholds.newFindings.high)
                    || (thresholds.newFindings.medium != null && currentDistribution.getMedium() > 0 && currentDistribution.getMedium() >= previousDistribution.getMedium() + thresholds.newFindings.medium)
                    || (thresholds.newFindings.low != null && currentDistribution.getLow() > 0 && currentDistribution.getLow() >= previousDistribution.getLow() + thresholds.newFindings.low)) {

                if (thresholds.newFindings.failBuild) {
                    return Result.FAILURE;
                } else  {
                    result = Result.UNSTABLE;
                }
            }
        }

        return result;
    }
}
