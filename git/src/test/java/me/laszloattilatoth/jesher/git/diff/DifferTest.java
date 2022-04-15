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

import me.laszloattilatoth.jesher.git.RepoCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DifferTest {

    @Test
    void run(@TempDir Path tempDir) throws IOException, InterruptedException {
        RepoCreator creator = new RepoCreator(tempDir.toFile());
        creator.create();
        creator.cherryPick();

        Mapper mapper = new Mapper();

        Differ differ = new Differ(tempDir.toFile(), RepoCreator.MASTER_BRANCH, RepoCreator.SECOND_BRANCH, mapper);
        DiffResult result = differ.run();
        assertEquals(RepoCreator.MASTER_BRANCH, result.localCommitId());
        assertEquals(RepoCreator.SECOND_BRANCH, result.upstreamCommitId());
        assertEqualsAsSet(Set.of("fourth.txt"), result.different());
        assertEqualsAsSet(Set.of("second.txt"), result.upstreamOnly());
        assertEqualsAsSet(Set.of("third.txt", "first.java"), result.same());
        assertEqualsAsSet(Set.of("sixth.txt", "fifth.txt"), result.localOnly());

        assertTrue(mapper.filenames.contains("sixth.txt"));

        // untouched or deleted files are not mapped
        assertFalse(mapper.filenames.contains("first.txt"));
        assertFalse(mapper.filenames.contains("second.txt"));
    }

    @Test
    void runWithDefaultMapper(@TempDir Path tempDir) throws IOException, InterruptedException {
        RepoCreator creator = new RepoCreator(tempDir.toFile());
        creator.create();
        creator.cherryPick();

        Mapper mapper = new Mapper();

        Differ differ = new Differ(tempDir.toFile(), RepoCreator.MASTER_BRANCH, RepoCreator.SECOND_BRANCH);
        DiffResult result = differ.run();
        assertEquals(RepoCreator.MASTER_BRANCH, result.localCommitId());
        assertEquals(RepoCreator.SECOND_BRANCH, result.upstreamCommitId());
        assertEqualsAsSet(Set.of("fourth.txt"), result.different());
        assertEqualsAsSet(Set.of("second.txt"), result.upstreamOnly());
        assertEqualsAsSet(Set.of("third.txt", "first.java"), result.same());
        assertEqualsAsSet(Set.of("sixth.txt", "fifth.txt"), result.localOnly());
    }

    private void assertEqualsAsSet(Set<String> expected, List<String> actual) {
        assertEquals(expected, new HashSet<>(actual));
    }

    private static class Mapper implements FilenameMapper {
        public ArrayList<String> filenames = new ArrayList<>();

        @Override
        public List<String> map(String filename) {
            filenames.add(filename);
            return FilenameMapper.super.map(filename);
        }
    }
}
