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
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@RequiredArgsConstructor
public class RiskGate implements Serializable {

    private static final long serialVersionUID = 171256230735670985L;

    @NonNull
    private final Thresholds thresholds;

    /**
     * Evaluates if the current results meet or exceed the defined threshold.
     **/
    public Result evaluate(@NonNull final SeverityDistribution currentDistribution,
                           @NonNull final SeverityDistribution previousDistribution,
                           @NonNull final ViolationDistribution currentViolationDistribution,
                           @NonNull final ViolationDistribution previousViolationDistribution)
    {
        Result result = Result.SUCCESS;
        if ((thresholds.totalFindings.failedCritical != null && currentDistribution.getCritical() > 0 && currentDistribution.getCritical() >= thresholds.totalFindings.failedCritical)
                || (thresholds.totalFindings.failedHigh != null && currentDistribution.getHigh() > 0 && currentDistribution.getHigh() >= thresholds.totalFindings.failedHigh)
                || (thresholds.totalFindings.failedMedium != null && currentDistribution.getMedium() > 0 && currentDistribution.getMedium() >= thresholds.totalFindings.failedMedium)
                || (thresholds.totalFindings.failedLow != null && currentDistribution.getLow() > 0 && currentDistribution.getLow() >= thresholds.totalFindings.failedLow)) {

            return Result.FAILURE;
        }
        if ((thresholds.totalFindings.unstableCritical != null && currentDistribution.getCritical() > 0 && currentDistribution.getCritical() >= thresholds.totalFindings.unstableCritical)
                || (thresholds.totalFindings.unstableHigh != null && currentDistribution.getHigh() > 0 && currentDistribution.getHigh() >= thresholds.totalFindings.unstableHigh)
                || (thresholds.totalFindings.unstableMedium != null && currentDistribution.getMedium() > 0 && currentDistribution.getMedium() >= thresholds.totalFindings.unstableMedium)
                || (thresholds.totalFindings.unstableLow != null && currentDistribution.getLow() > 0 && currentDistribution.getLow() >= thresholds.totalFindings.unstableLow)) {

            result = Result.UNSTABLE;
        }

        if ((thresholds.newFindings.failedCritical != null && currentDistribution.getCritical() > 0 && currentDistribution.getCritical() >= previousDistribution.getCritical() + thresholds.newFindings.failedCritical)
                || (thresholds.newFindings.failedHigh != null && currentDistribution.getHigh() > 0 && currentDistribution.getHigh() >= previousDistribution.getHigh() + thresholds.newFindings.failedHigh)
                || (thresholds.newFindings.failedMedium != null && currentDistribution.getMedium() > 0 && currentDistribution.getMedium() >= previousDistribution.getMedium() + thresholds.newFindings.failedMedium)
                || (thresholds.newFindings.failedLow != null && currentDistribution.getLow() > 0 && currentDistribution.getLow() >= previousDistribution.getLow() + thresholds.newFindings.failedLow)) {

            return Result.FAILURE;
        }
        if ((thresholds.newFindings.unstableCritical != null && currentDistribution.getCritical() > 0 && currentDistribution.getCritical() >= previousDistribution.getCritical() + thresholds.newFindings.unstableCritical)
                || (thresholds.newFindings.unstableHigh != null && currentDistribution.getHigh() > 0 && currentDistribution.getHigh() >= previousDistribution.getHigh() + thresholds.newFindings.unstableHigh)
                || (thresholds.newFindings.unstableMedium != null && currentDistribution.getMedium() > 0 && currentDistribution.getMedium() >= previousDistribution.getMedium() + thresholds.newFindings.unstableMedium)
                || (thresholds.newFindings.unstableLow != null && currentDistribution.getLow() > 0 && currentDistribution.getLow() >= previousDistribution.getLow() + thresholds.newFindings.unstableLow)) {

            result = Result.UNSTABLE;
        }

        if ((thresholds.totalFindings.failedCritical != null && currentViolationDistribution.getFail() > 0 && currentViolationDistribution.getFail() >= thresholds.totalFindings.failedCritical)
                || (thresholds.totalFindings.failedHigh != null && currentViolationDistribution.getFail() > 0 && currentViolationDistribution.getFail() >= thresholds.totalFindings.failedHigh)
                || (thresholds.totalFindings.failedMedium != null && currentViolationDistribution.getWarn() > 0 && currentViolationDistribution.getWarn() >= thresholds.totalFindings.failedMedium)
                || (thresholds.totalFindings.failedLow != null && currentViolationDistribution.getInfo() > 0 && currentViolationDistribution.getInfo() >= thresholds.totalFindings.failedLow)) {

            return Result.FAILURE;
        }
        if ((thresholds.totalFindings.unstableCritical != null && currentViolationDistribution.getFail() > 0 && currentViolationDistribution.getFail() >= thresholds.totalFindings.unstableCritical)
                || (thresholds.totalFindings.unstableHigh != null && currentViolationDistribution.getFail() > 0 && currentViolationDistribution.getFail() >= thresholds.totalFindings.unstableHigh)
                || (thresholds.totalFindings.unstableMedium != null && currentViolationDistribution.getWarn() > 0 && currentViolationDistribution.getWarn() >= thresholds.totalFindings.unstableMedium)
                || (thresholds.totalFindings.unstableLow != null && currentViolationDistribution.getInfo() > 0 && currentViolationDistribution.getInfo() >= thresholds.totalFindings.unstableLow)) {

            result = Result.UNSTABLE;
        }

        if ((thresholds.newFindings.failedCritical != null && currentViolationDistribution.getFail() > 0 && currentViolationDistribution.getFail() >= previousViolationDistribution.getFail() + thresholds.newFindings.failedCritical)
                || (thresholds.newFindings.failedHigh != null && currentViolationDistribution.getFail() > 0 && currentViolationDistribution.getFail() >= previousViolationDistribution.getFail() + thresholds.newFindings.failedHigh)
                || (thresholds.newFindings.failedMedium != null && currentViolationDistribution.getWarn() > 0 && currentViolationDistribution.getWarn() >= previousViolationDistribution.getWarn() + thresholds.newFindings.failedMedium)
                || (thresholds.newFindings.failedLow != null && currentViolationDistribution.getInfo() > 0 && currentViolationDistribution.getInfo() >= previousViolationDistribution.getInfo() + thresholds.newFindings.failedLow)) {

            return Result.FAILURE;
        }
        if ((thresholds.newFindings.unstableCritical != null && currentViolationDistribution.getFail() > 0 && currentViolationDistribution.getFail() >= previousViolationDistribution.getFail() + thresholds.newFindings.unstableCritical)
                || (thresholds.newFindings.unstableHigh != null && currentViolationDistribution.getFail() > 0 && currentViolationDistribution.getFail() >= previousViolationDistribution.getFail() + thresholds.newFindings.unstableHigh)
                || (thresholds.newFindings.unstableMedium != null && currentViolationDistribution.getWarn() > 0 && currentViolationDistribution.getWarn() >= previousViolationDistribution.getWarn() + thresholds.newFindings.unstableMedium)
                || (thresholds.newFindings.unstableLow != null && currentViolationDistribution.getInfo() > 0 && currentViolationDistribution.getInfo() >= previousViolationDistribution.getInfo() + thresholds.newFindings.unstableLow)) {

            result = Result.UNSTABLE;
        }

        return result;
    }
}
