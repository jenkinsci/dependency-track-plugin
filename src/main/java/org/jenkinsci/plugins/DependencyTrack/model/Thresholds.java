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

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class Thresholds implements Serializable {
    private static final long serialVersionUID = 4436355058848964587L;
    public final TotalFindings totalFindings = new TotalFindings();
    public final NewFindings newFindings = new NewFindings();
    public final TotalViolations totalViolations = new TotalViolations();
    public final NewViolations newViolations = new NewViolations();

    @EqualsAndHashCode
    @ToString
    public static class TotalFindings implements Serializable {
        private static final long serialVersionUID = 9090012675204711616L;
        public Integer unstableCritical;
        public Integer unstableHigh;
        public Integer unstableMedium;
        public Integer unstableLow;
        public Integer unstableUnassigned;
        public Integer failedCritical;
        public Integer failedHigh;
        public Integer failedMedium;
        public Integer failedLow;
        public Integer failedUnassigned;
    }

    @EqualsAndHashCode
    @ToString
    public static class NewFindings implements Serializable {
        private static final long serialVersionUID = -8053746187421628780L;
        public Integer unstableCritical;
        public Integer unstableHigh;
        public Integer unstableMedium;
        public Integer unstableLow;
        public Integer unstableUnassigned;
        public Integer failedCritical;
        public Integer failedHigh;
        public Integer failedMedium;
        public Integer failedLow;
        public Integer failedUnassigned;
    }

    @EqualsAndHashCode
    @ToString
    public static class TotalViolations implements Serializable {
        private static final long serialVersionUID = -7913629278653091088L;
        public Integer unstableFail;
        public Integer unstableWarn;
        public Integer unstableInfo;
        public Integer failedFail;
        public Integer failedWarn;
        public Integer failedInfo;
    }

    @EqualsAndHashCode
    @ToString
    public static class NewViolations implements Serializable {
        private static final long serialVersionUID = -3053747350725113641L;
        public Integer unstableFail;
        public Integer unstableWarn;
        public Integer unstableInfo;
        public Integer failedFail;
        public Integer failedWarn;
        public Integer failedInfo;
    }
}
