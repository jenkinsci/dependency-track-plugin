/*
 * Copyright 2020 OWASP.
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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
class ConsoleLoggerTest {

    @Test
    void testLog() throws IOException {
        PrintStream ps = mock(PrintStream.class);
        ConsoleLogger uut = new ConsoleLogger(ps);
        uut.log("test\r\nline2");
        verify(ps).println("[DependencyTrack] test\r\n[DependencyTrack] line2");
    }
    
    @Test
    void testWrite() throws IOException {
        PrintStream ps = spy(new PrintStream(System.err));
        ConsoleLogger uut = new ConsoleLogger(ps);
        uut.write("test\nline2\n".getBytes());
        uut.flush();
        verify(ps, times(2)).append("[DependencyTrack] ");
        verify(ps).write(Arrays.copyOf("line2\n".getBytes(), 32), 0, 6);
    }
}
