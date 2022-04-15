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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class FilenameMapperTest {

    @Test
    void mapDefaultImpl() {
        FilenameMapper m = new FilenameMapper() {
        };

        // same as in ApacheHiveFilenameMapperTest
        assertThat(m.map("something.java"), is(List.of("something.java")));
        assertThat(m.map("standalone-metastore/whatever/something.java"),
                is(List.of(
                        "standalone-metastore/whatever/something.java"
                )));

        assertThat(m.map("standalone-metastore/src/main/java/org/apache/hadoop/hive/metastore/HiveMetaStore.java"),
                is(List.of(
                        "standalone-metastore/src/main/java/org/apache/hadoop/hive/metastore/HiveMetaStore.java"
                )));
    }

    @Test
    void overridenMap() {
        FilenameMapper m = new FilenameMapper() {
            @Override
            public List<String> map(String filename) {
                return List.of(filename, "42");
            }
        };

        assertThat(m.map("something.java"), is(List.of("something.java", "42")));
    }
}
