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

public class Thresholds {

    public final TotalFindings totalFindings = new TotalFindings();
    public final NewFindings newFindings = new NewFindings();

    public class TotalFindings {
        public int critical;
        public int high;
        public int medium;
        public int low;
        public int all;
        public boolean limitToAnalysisExploitable;
        public boolean failBuild;
    }

    public class NewFindings {
        public int critical;
        public int high;
        public int medium;
        public int low;
        public int all;
        public boolean limitToAnalysisExploitable;
        public boolean failBuild;
    }
}
