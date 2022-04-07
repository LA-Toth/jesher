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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GitTest {
    @TempDir
    Path repoPath;
    File repo;
    RepoCreator creator;

    @BeforeEach
    void setupRepo() throws IOException, InterruptedException {
        repo = repoPath.toFile();
        creator = new RepoCreator(repo);
        creator.create();
    }

    @Test
    void repoRoot() throws IOException {
        String absPath = repo.getAbsolutePath();
        assertEquals(repo.getCanonicalPath(), Git.repoRoot(repo));
        assertEquals(repo.getCanonicalPath(), Git.repoRoot(new File(absPath + "/subdir")));
    }

    @Test
    void repoRootInNonGitDir(@TempDir Path anotherTemp) throws IOException {
        assertThat(Git.repoRoot(anotherTemp.toFile()), is(anotherTemp.toRealPath().toString()));
    }

    @Test
    void currentBranch() throws IOException, InterruptedException {
        assertThat(Git.currentBranch(repo), is(RepoCreator.MASTER_BRANCH));
        creator.run("git", "checkout", RepoCreator.SECOND_BRANCH);
        assertThat(Git.currentBranch(repo), is(RepoCreator.SECOND_BRANCH));
        creator.run("git", "checkout", Git.commitIdOfGitRef(repo, RepoCreator.SECOND_BRANCH + "~1"));
        assertThat(Git.currentBranch(repo), is(nullValue()));
    }

    @Test
    void currentHead() throws IOException, InterruptedException {
        assertThat(Git.currentHead(repo), is(Git.commitIdOfGitRef(repo, RepoCreator.MASTER_BRANCH)));
        assertThat(Git.currentHead(repo), not(Git.commitIdOfGitRef(repo, RepoCreator.SECOND_BRANCH)));
        creator.run("git", "checkout", RepoCreator.SECOND_BRANCH);
        assertThat(Git.currentHead(repo), is(Git.commitIdOfGitRef(repo, RepoCreator.SECOND_BRANCH)));

        creator.run("git", "checkout", Git.commitIdOfGitRef(repo, RepoCreator.SECOND_BRANCH + "~1"));
        assertThat(Git.currentHead(repo), is(Git.commitIdOfGitRef(repo, RepoCreator.MASTER_BRANCH)));
    }

    @Test
    void currentHeadIsSameAsCommitIdOfCurrentBranch() throws IOException {
        assertThat(Git.currentHead(repo), is(Git.commitIdOfGitRef(repo, RepoCreator.MASTER_BRANCH)));
        assertThat(Git.currentHead(repo), is(Git.commitIdOfGitRef(repo, Git.currentBranch(repo))));
    }

    @Test
    void mergeBase() throws IOException, InterruptedException {
        assertThat(Git.mergeBase(repo, RepoCreator.MASTER_BRANCH, RepoCreator.SECOND_BRANCH), is(Git.commitIdOfGitRef(repo, RepoCreator.MASTER_BRANCH)));
        creator.cherryPick();
        assertThat(Git.mergeBase(repo, RepoCreator.MASTER_BRANCH, RepoCreator.SECOND_BRANCH), is(Git.commitIdOfGitRef(repo, RepoCreator.MASTER_BRANCH + "~1")));
        assertThat(Git.mergeBase(repo, RepoCreator.MASTER_BRANCH, RepoCreator.MASTER_BRANCH), is(Git.commitIdOfGitRef(repo, RepoCreator.MASTER_BRANCH)));
        assertThat(Git.mergeBase(repo, RepoCreator.MASTER_BRANCH, "whatever"), is(nullValue()));
    }

    @Test
    void isExistingLocalBranch() throws IOException {
        assertThat(Git.isExistingLocalBranch(repo, "some-th-ing-nonexistent"), is(false));
        assertThat(Git.isExistingLocalBranch(repo, RepoCreator.MASTER_BRANCH), is(true));
        assertThat(Git.isExistingLocalBranch(repo, RepoCreator.SECOND_BRANCH), is(true));
    }

    @Test
    void getCommitDetails() {
    }

    @Test
    void findCommitsByMessagePart() {
    }

    @Test
    void getContainingRefsOfCommit() {
    }

    @Test
    void commitIdOfGitRef() {
    }

    @Test
    void getDistance() {
    }
}
