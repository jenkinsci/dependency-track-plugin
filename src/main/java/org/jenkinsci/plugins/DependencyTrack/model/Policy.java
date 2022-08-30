/*
 * Copyright 2022 OWASP.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.DependencyTrack.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.io.Serializable;
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Value
public class Policy implements Serializable
{
  private static final long serialVersionUID = 3181011438478413504L;

  String uuid;
  String name;

  ViolationState violationState;
  int violationStateRank;

  public static Policy of (final String uuid,
                           final String name,
                           final ViolationState violationState)
  {
    return new Policy(uuid, name, violationState, violationState.getRank());
  }
}
