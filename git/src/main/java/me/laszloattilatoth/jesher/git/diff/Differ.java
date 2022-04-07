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

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import me.laszloattilatoth.jesher.git.Git;
import me.laszloattilatoth.jesher.util.ProcessHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Differ {
    private final File repository;
    private final String localCommitId;
    private final String upstreamCommitId;
    private final FilenameMapper filenameMapper;

    private final ArrayList<String> localFiles = new ArrayList<>();
    private final ArrayList<String> upstreamFiles = new ArrayList<>();

    private final ArrayList<String> resultSameFiles = new ArrayList<>();
    private final ArrayList<String> resultDifferentFiles = new ArrayList<>();
    private final ArrayList<String> resultLocalOnly = new ArrayList<>();
    private final ArrayList<String> resultUpstreamOnly = new ArrayList<>();

    public Differ(File repository, String localCommitId, String upstreamCommitId, FilenameMapper filenameMapper) throws IOException {
        this.repository = new File(Git.repoRoot(repository));
        this.localCommitId = localCommitId;
        this.upstreamCommitId = upstreamCommitId;
        this.filenameMapper = filenameMapper;
    }

    public Differ(File repository, String localCommitId, String upstreamCommitId) throws IOException {
        this(repository, localCommitId, upstreamCommitId, new FilenameMapper() {
        });
    }

    public DiffResult run() throws IOException {
        loadFileLists();
        compareFiles();

        return new DiffResult(localCommitId, upstreamCommitId, resultSameFiles, resultDifferentFiles, resultLocalOnly, resultUpstreamOnly);
    }

    private void loadFileLists() throws IOException {
        if (upstreamCommitId != null) {
            upstreamFiles.addAll(ProcessHelper.getOutputReader(repository, "git", "diff-tree", "--no-commit-id", "--name-only", "-r", "-M", upstreamCommitId).lines().toList());
        }
        localFiles.addAll(ProcessHelper.getOutputReader(repository, "git", "diff-tree", "--no-commit-id", "--name-only", "-r", "-M", localCommitId).lines().toList());
    }

    private void compareFiles() throws IOException {
        ArrayList<String> mayUpstreamOnly = new ArrayList<>(upstreamFiles);
        for (var localFilename : localFiles) {
            boolean processed = false;
            for (var upstreamFilename : filenameMapper.map(localFilename)) {
                if (mayUpstreamOnly.contains(upstreamFilename)) {
                    // if (localFilename.equals("standalone-metastore/src/main/java/org/apache/hadoop/hive/metastore/MetastoreDefaultTransformer.java"))
                    compareFile(localFilename, upstreamFilename);
                    mayUpstreamOnly.remove(upstreamFilename);
                    processed = true;
                }
            }
            if (!processed)
                resultLocalOnly.add(localFilename);
        }
        resultUpstreamOnly.addAll(mayUpstreamOnly);
    }

    /**
     * Compares file difference in local (downstream) and upstream commit, and stores result.
     * <p>
     * If the file change is the same, it goes into the "same" list of files and in @{link #run}'s return value
     * into {@link DiffResult#same()}. If different, similarly, to {@link DiffResult#different()}.
     * <p>
     * The default implementation checks only the added/removed lines without context, assuming that
     * the commits are good, and it is used only to help the manual review (to see which files
     * are to be reviewed and which are not needed to be reviewed, etc.). A stricter comparison
     * can be implemented via inheritance.
     *
     * @param localFilename    The filename used in {@link #localCommitId}
     * @param upstreamFilename The mapped filename used in {@link #localCommitId} (@see {@link FilenameMapper}).
     * @throws IOException Thrown if the file diff cannot be loaded or other issue occurs.
     */
    protected void compareFile(String localFilename, String upstreamFilename) throws IOException {
        Patch<String> diff = DiffUtils.diff(loadLines(localCommitId, localFilename), loadLines(upstreamCommitId, upstreamFilename));
        List<String> localLines = new ArrayList<>();
        List<String> upstreamLines = new ArrayList<>();
        for (AbstractDelta<String> delta : diff.getDeltas()) {
            delta.getSource().getLines().forEach(x -> {
                if (x.matches("^[+-]([^+-].*)?$")) {
                    localLines.add(x);
                }
            });
            delta.getTarget().getLines().forEach(x -> {
                if (x.matches("^[+-]([^+-].*)?")) {
                    upstreamLines.add(x);
                }
            });
        }

        Patch<String> finalDiff = DiffUtils.diff(localLines, upstreamLines);
        if (finalDiff.getDeltas().size() > 0) {
            resultDifferentFiles.add(localFilename);
        } else {
            resultSameFiles.add(localFilename);
        }
    }

    /**
     * Loads the diff of a file from a commit and strips leading whitespaces if it's a Java file by `git show`.
     *
     * @param commit   The commit passed to `git`.
     * @param filename The filename passed to `git`.
     * @return The loaded diff as a list of strings, including the commit message.
     * @throws IOException Thrown if the `git` command fails.
     */
    protected List<String> loadLines(String commit, String filename) throws IOException {
        BufferedReader reader = ProcessHelper.getOutputReader(repository, "git", "show", "--no-decorate", "--pretty=format:", commit, "--", filename);
        if (filename.endsWith(".java")) {
            //remove indentation changes
            return reader.lines().map((x) -> x.replaceFirst("^([+-])[ \t]+", "$1 ")).toList();
        } else {
            return reader.lines().collect(Collectors.toList());
        }
    }
}
