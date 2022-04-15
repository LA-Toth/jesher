/*
 *  Copyright 2022 Laszlo Attila Toth
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package me.laszloattilatoth.jesher.git.diff;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class DiffResultTest {
    List<String> localOnly = List.of("first", "second.txt", "third");
    List<String> upstreamOnly = List.of("checksums.txt", "LICENCE");
    List<String> same = List.of("git.c", "stdio.h");
    List<String> different = List.of("Differ.java", "SshProxy.java");

    DiffResult result = new DiffResult("local", "upstreamId", same, different, localOnly, upstreamOnly);

    @Test
    void testListsReturnSameValuesAsProvided() {
        assertThat(result.same(), is(same));
        assertThat(result.different(), is(different));
        assertThat(result.localOnly(), is(localOnly));
        assertThat(result.upstreamOnly(), is(upstreamOnly));
        assertThat(result.isSameAsUpstream(), is(false));
    }

    @Test
    void testDefaultToMapReturnsSameValues() {
        Map<String, List<String>> map = result.toMap();
        assertThat(map.size(), is(4));
        assertThat(map.get("same"), is(same));
        assertThat(map.get("different"), is(different));
        assertThat(map.get("local_only"), is(localOnly));
        assertThat(map.get("upstream_only"), is(upstreamOnly));
    }

    @Test
    void testToMapReturnsSameValues() {
        Map<String, List<String>> map = result.toMap(new DiffResultKeyProvider() {
        });
        assertThat(map.size(), is(4));
        assertThat(map.get("same"), is(same));
        assertThat(map.get("different"), is(different));
        assertThat(map.get("local_only"), is(localOnly));
        assertThat(map.get("upstream_only"), is(upstreamOnly));
    }

    @Test
    void testToMapDuplicatesLists() {
        Map<String, List<String>> map = result.toMap(new DiffResultKeyProvider() {
        });

        assertNotSame(same, map.get("same"));
        assertNotSame(different, map.get("different"));
        assertNotSame(localOnly, map.get("localOnly"));
        assertNotSame(upstreamOnly, map.get("upstream_only"));
    }

    @Test
    void testToMapWithDownstreamKeyProvider() {
        Map<String, List<String>> map = result.toMap(new DiffResultWithDownstreamKeyProvider());
        assertThat(map.size(), is(4));
        assertThat(map.get("same"), is(same));
        assertThat(map.get("different"), is(different));
        assertThat(map.get("downstream_only"), is(localOnly));
        assertThat(map.get("upstream_only"), is(upstreamOnly));
    }

    @Test
    void isSameAsUpstream() {
        assertThat(result.isSameAsUpstream(), is(false));
        assertIsSameAsUpstream(same, List.of(), List.of(), List.of(), true);
        assertIsSameAsUpstream(same, different, List.of(), List.of(), false);
        assertIsSameAsUpstream(same, List.of(), localOnly, List.of(), false);
        assertIsSameAsUpstream(same, List.of(), List.of(), upstreamOnly, false);
        assertIsSameAsUpstream(List.of(), List.of(), List.of(), List.of(), false); // empty?!?!
        assertIsSameAsUpstream(List.of(), different, List.of(), List.of(), false);
        assertIsSameAsUpstream(List.of(), List.of(), localOnly, List.of(), false);
        assertIsSameAsUpstream(List.of(), List.of(), List.of(), upstreamOnly, false);
    }

    private void assertIsSameAsUpstream(List<String> same, List<String> different,
                                        List<String> localOnly, List<String> upstreamOnly,
                                        boolean expected) {
        assertThat(new DiffResult("local", "upstreamId", same, different, localOnly, upstreamOnly).isSameAsUpstream(),
                is(expected));
    }
}
