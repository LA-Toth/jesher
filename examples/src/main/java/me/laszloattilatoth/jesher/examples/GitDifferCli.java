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

package me.laszloattilatoth.jesher.examples;

import me.laszloattilatoth.jesher.git.diff.DiffResult;
import me.laszloattilatoth.jesher.git.diff.DiffResultWithDownstreamKeyProvider;
import me.laszloattilatoth.jesher.git.diff.Differ;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "git-differ", mixinStandardHelpOptions = true, description = "Compare two git commits")

public class GitDifferCli implements Callable<Integer> {
    @Parameters(index = "0", description = "Repository or its subdirectory")
    private File repository;

    @Parameters(index = "1", description = "Local commit ID")
    private String localCommitId;
    @Parameters(index = "2", description = "Upstream commit ID")
    private String upstreamCommitId;

    public static void main(String... args) {
        int exitCode = new CommandLine(new GitDifferCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        Differ differ = new Differ(repository, localCommitId, upstreamCommitId);
        DiffResult result = differ.run();
        System.out.printf("Diff result: %s%n", result);
        System.out.println(" ... as key-value pairs");
        result.toMap(new DiffResultWithDownstreamKeyProvider()).forEach(
                (k, v) -> {
                    System.out.printf(" %s = (size: %d)%n", k, v.size());
                    v.forEach(f -> System.out.printf("    --> %s%n", f));
                });

        return 0;
    }
}
