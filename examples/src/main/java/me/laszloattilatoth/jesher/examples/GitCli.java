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

import me.laszloattilatoth.jesher.git.Git;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "git-info", mixinStandardHelpOptions = true, description = "Show details of a repository")
public class GitCli implements Callable<Integer> {
    @Parameters(index = "0", description = "Repository or its subdirectory")
    private File repository;

    public static void main(String... args) {
        int exitCode = new CommandLine(new GitCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        System.out.printf("Repository root: %s%n", Git.repoRoot(repository));
        String currentBranch = Git.currentBranch(repository);
        System.out.printf("Current branch : %s%n", currentBranch != null ? currentBranch : "(not on a branch)");
        System.out.printf("Current commit : %s%n", Git.currentHead(repository));
        System.out.printf("Details of HEAD: %s%n", Git.getCommitDetails(repository, "HEAD"));
        System.out.println(" ... as key-value pairs");
        Git.getCommitDetails(repository, "HEAD").toMap().forEach((k, v) -> System.out.printf(" %s = %s%n", k, v));

        return 0;
    }
}
