/*
 * Copyright 2022  Laszlo Attila Toth
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

package me.laszloattilatoth.jesher.git;

import me.laszloattilatoth.jesher.util.ProcessHelper;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * A generic 'git' wrapper wrapping common or useful 'git' commands.
 * Each command has two versions, one is with a specified directory (first parameter
 * as {@link java.io.File}), and the other is without it, running in current directory.
 */
public class Git {

    private Git() {}

    private static String getOutputLine(File directory, String... args) throws IOException {
        return ProcessHelper.getOutputLine(directory, args);
    }

    public static String repoRoot(File directory) throws IOException {
        return getOutputLine(directory, "git", "rev-parse", "--show-toplevel");
    }

    public static String currentBranch(File directory) throws IOException {
        return getOutputLine(directory, "git", "branch", "--show-current");
    }

    public static String currentHead(File directory) throws IOException {
        return commitIdOfGitRef(directory, "HEAD");
    }

    public static String commitIdOfGitRef(File directory, String ref) throws IOException {
        return getOutputLine(directory, "git", "rev-list", "--max-count=1", ref);
    }

    public static String mergeBase(File directory, String ref1, String ref2) throws IOException {
        return getOutputLine(directory, "git", "merge-base", ref1, ref2);
    }

    public static boolean isExistingLocalBranch(File directory, String branchName) throws IOException {
        return getOutputLine(directory, "git", "branch", "--list", branchName) != null;
    }

    public static Commit getCommitDetails(File directory, String commitId) throws IOException {
        Stream<String> lines = ProcessHelper.getOutputReader(directory, "git", "show", "-s", "--format=%at%n%an <%ae>%n%ct%n%cn <%ce>%n%s", commitId).lines();
        Collector<String, ?, ArrayList<String>> toList =
                Collector.of(ArrayList::new, ArrayList::add,
                        (left, right) -> {
                            left.addAll(right);
                            return left;
                        });

        ArrayList<String> collected = lines.collect(toList);
        return new Commit(
                commitIdOfGitRef(directory, commitId),
                collected.get(1),
                tsToZonedDT(collected.get(0)),
                collected.get(3),
                tsToZonedDT(collected.get(2)),
                collected.get(4)
        );
    }

    private static ZonedDateTime tsToZonedDT(String timestamp) {
        return tsToZonedDT(Long.parseLong(timestamp));
    }

    private static ZonedDateTime tsToZonedDT(long timestamp) {
        return Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault());
    }

    public static int getDistance(File directory, String commit, String commitFrom) throws IOException {
        return Integer.parseInt(getOutputLine(directory, "git", "rev-list", "--count", commit, commitFrom));
    }

    public static List<String> findCommitsByMessagePart(File directory, String part) throws IOException {
        return ProcessHelper.getOutputReader(directory, "git", "-c", "log.decorate=", "log", "--pretty=%H %s", "--all", "--grep", part)
                .lines()
                .toList();
    }

    public static List<String> getContainingRefsOfCommit(File directory, String commitId) throws IOException {
        return ProcessHelper.getOutputReader(directory, "git", "branch", "--format", "%(refname)", "--all", "--contains", commitId)
                .lines()
                .toList();
    }
}
