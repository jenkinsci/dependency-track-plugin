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
package org.jenkinsci.plugins.DependencyTrack;

import hudson.util.ReflectionUtils;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
class ProjectPropertiesTest {

    @Test
    void testSetTags() {
        ProjectProperties uut = new ProjectProperties();

        assertThat(uut.getTags()).isNotNull();
        uut.setTags(null);
        assertThat(uut.getTags()).isNotNull();

        uut.setTags("tag2 abc\ttag1 tag1");
        assertThat(uut.getTags()).containsExactly("abc", "tag1", "tag2");
        assertThat(uut.getTagsAsText()).isEqualTo("abc%stag1%stag2", System.lineSeparator(), System.lineSeparator());

        uut.setTags(new String[]{"tag2", "tag1"});
        assertThat(uut.getTags()).containsExactly("tag1", "tag2");

        uut.setTags(Stream.of("TAG2", "tag2").toList());
        assertThat(uut.getTags()).containsExactly("tag2");

        uut.setTags(Stream.of("TAG2", "tag2").collect(Collectors.toSet()));
        assertThat(uut.getTags()).containsExactly("tag2");

        assertThatCode(() -> uut.setTags(1)).isInstanceOf(IllegalArgumentException.class);

        Set<Long> setOfWrongType = Set.of(1L);
        assertThatCode(() -> uut.setTags(setOfWrongType)).isInstanceOf(IllegalArgumentException.class);

        List<Long> listOfWrongType = List.of(1L);
        assertThatCode(() -> uut.setTags(listOfWrongType)).isInstanceOf(IllegalArgumentException.class);

        List<Character> listOfWrongType2 = List.of('a');
        assertThatCode(() -> uut.setTags(listOfWrongType2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifyEmptyStringsShallBeNull() {
        ProjectProperties uut = new ProjectProperties();
        uut.setDescription("");
        uut.setGroup("\t");
        uut.setSwidTagId(System.lineSeparator());
        uut.setParentId("       ");
        assertThat(uut.getDescription()).isNull();
        assertThat(uut.getGroup()).isNull();
        assertThat(uut.getSwidTagId()).isNull();
        assertThat(uut.getParentId()).isNull();
    }

    @Nested
    class DescriptorImplTest {

        @Test
        void doFillParentIdItemsTest() throws Exception {
            Field instanceField = ReflectionUtils.findField(Jenkins.class, "theInstance", Jenkins.class);
            ReflectionUtils.makeAccessible(instanceField);
            Jenkins origJenkins = (Jenkins) instanceField.get(null);
            Jenkins mockJenkins = mock(Jenkins.class);
            ReflectionUtils.setField(instanceField, null, mockJenkins);
            org.jenkinsci.plugins.DependencyTrack.DescriptorImpl descriptorMock = mock(org.jenkinsci.plugins.DependencyTrack.DescriptorImpl.class);
            when(mockJenkins.getDescriptorByType(org.jenkinsci.plugins.DependencyTrack.DescriptorImpl.class)).thenReturn(descriptorMock);
            ProjectProperties.DescriptorImpl uut = new ProjectProperties.DescriptorImpl();

            uut.doFillParentIdItems("url", "key", null);

            ReflectionUtils.setField(instanceField, null, origJenkins);
            verify(mockJenkins).getDescriptorByType(org.jenkinsci.plugins.DependencyTrack.DescriptorImpl.class);
            verify(descriptorMock).doFillProjectIdItems("url", "key", null);
        }
    }
}
