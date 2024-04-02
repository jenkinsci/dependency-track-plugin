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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@RequiredArgsConstructor
@EqualsAndHashCode
@Getter
@ToString
public class ViolationDistribution implements Serializable
{
    private static final long serialVersionUID = -8075373711367835551L;

    private final int buildNumber;
    private int fail;
    private int warn;
    private int info;
    private int unassigned;

    public void add(final ViolationState violationState)
    {
        switch (violationState)
        {
            case FAIL:
                fail++;
                break;
            case WARN:
                warn++;
                break;
            case INFO:
                info++;
                break;
            default:
                unassigned++;
                break;
        }
    }
}
