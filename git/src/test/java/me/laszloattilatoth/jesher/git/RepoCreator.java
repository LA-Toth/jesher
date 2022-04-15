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

package me.laszloattilatoth.jesher.git;

import me.laszloattilatoth.jesher.util.ProcessHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Create a predefined git repository with branches and commits.
 * The repository can be used eg. for the DifferTest.
 */
public class RepoCreator {
    public static final String CONTENT_FMT = "A line\nAnother line\n%s\nLast fixed line @#\txa. Text.\n%s";
    public static final String MASTER_BRANCH = "master";
    public static final String SECOND_BRANCH = "second-branch";

    private final File repository;

    public RepoCreator(File repository) {
        this.repository = repository;
    }

    public RepoCreator(String repository) {
        this(new File(repository));
    }

    public void create() throws IOException, InterruptedException {
        repository.mkdirs();
        run("git", "init");
        run("git", "config", "init.defaultbranch", MASTER_BRANCH);
        writeToFile("first.txt", "", "");
        Files.createDirectory(Paths.get(repository.getAbsolutePath() + "/subdir"));
        writeToFile("subdir/sub1st.txt", "firstpart", "");
        run("git", "add", ".");
        run("git", "commit", "-m", "initial commit");
        run("git", "checkout", "-b", SECOND_BRANCH);
        writeToFile("second.txt", "hello", "world\n");
        writeToFile("third.txt", "", "world\n");
        writeToFile("fourth.txt", "HeLlO", "world\n");
        writeToFile("first.java", "   HeLlO", "world\n");
        run("git", "add", ".");
        run("git", "commit", "-m", "second commit");
        run("git", "checkout", MASTER_BRANCH);
    }

    public void cherryPick() throws IOException, InterruptedException {
        run("git", "cherry-pick", "-x", RepoCreator.SECOND_BRANCH);
        assert new File(repository.getAbsolutePath() + "/second.txt").delete();
        writeToFile("fourth.txt", "something", "TestContent\n");
        writeToFile("fifth.txt", "something", "");
        writeToFile("sixth.txt", "something", "");
        run("git", "add", ".");
        run("git", "rm", "second.txt");
        run("git", "commit", "--amend", "-C", "HEAD");
    }

    public int run(String... args) throws IOException, InterruptedException {
        return ProcessHelper.runWithoutOutput(repository, args);
    }

    public void writeToFile(String filename, String first, String second) throws IOException {
        FileWriter writer = new FileWriter(repository.getAbsolutePath() + "/" + filename);
        writer.write(String.format(CONTENT_FMT, first, second));
        writer.close();
    }
}
