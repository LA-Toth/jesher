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

package me.laszloattilatoth.jesher.gerrit;

import me.laszloattilatoth.jesher.util.Convert;
import me.laszloattilatoth.jesher.util.Fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Interacts with Gerrit via SSH or the not-so-REST API.
 */
public class Gerrit {
    private final String host;
    private final int sshPort;
    private final String sshUser;
    private final String sshIdentityFile;
    private final int webPort;
    private final boolean useHttps;
    private final boolean dryRun;

    private final List<String> sshCommand = new ArrayList<>();

    private Gerrit(String host, int sshPort, String sshUser, String sshIdentityFile, int webPort, boolean useHttps, boolean dryRun) {
        this.host = host;
        this.sshPort = sshPort;
        this.sshUser = sshUser;
        this.sshIdentityFile = sshIdentityFile;
        this.webPort = webPort;
        this.useHttps = useHttps;
        this.dryRun = dryRun;

        if (sshUser != null)
            createSshCommand();
    }

    private void createSshCommand() {
        sshCommand.addAll(List.of("ssh", "-p", Integer.toString(sshPort)));
        if (sshIdentityFile != null) {
            sshCommand.addAll(List.of("-i", sshIdentityFile));
        }
        sshCommand.addAll(List.of(String.format("%s@%s", sshUser, host), "gerrit", "review", "--json"));
    }

    public void sendReview(int changeId, int revision, ReviewMessage reviewMessage) throws IOException, InterruptedException {
        sendReview(changeId, revision, reviewMessage, false);
    }

    public void sendReview(int changeId, int revision, ReviewMessage reviewMessage, boolean withoutLabels) throws IOException, InterruptedException {
        if (dryRun)
            sendReviewDryRun(changeId, revision, reviewMessage, withoutLabels);
        else
            sendReview(changeId, revision, Convert.toJson(reviewMessage.toGerritReview(withoutLabels)));
    }

    private void sendReviewDryRun(int changeId, int revision, ReviewMessage reviewMessage, boolean withoutLabels) {
        System.out.printf("Dry run review on change: %d,%d%n", changeId, revision);
        System.out.println(reviewMessage.get());
        System.out.println("Labels:");

        if (!withoutLabels && reviewMessage.hasLabels()) {
            reviewMessage.getLabels().forEach((label, value) -> System.out.printf("--> %s: %s%n", label, value));
        } else {
            System.out.printf("--> %s: (none)%n", ReviewMessage.CODE_REVIEW_LABEL);
        }
    }

    private void sendReview(int changeId, int revision, String reviewMessage) throws IOException, InterruptedException {
        if (sshCommand.isEmpty()) {
            throw new RuntimeException("SSH command is not configured for gerrit review, probably SSH username is unset");
        }
        List<String> fullComand = new ArrayList<>(sshCommand);
        fullComand.add(Integer.toString(changeId));
        fullComand.add(Integer.toString(revision));

        ProcessBuilder builder = new ProcessBuilder(fullComand);
        builder.redirectInput(ProcessBuilder.Redirect.PIPE);
        Process p = builder.start();
        p.outputWriter().write(reviewMessage);
        p.waitFor();
    }

    public String host() {
        return host;
    }

    public String getUrl() {
        StringBuilder result = new StringBuilder();
        result.append(useHttps ? "https://" : "http://");
        result.append(host);
        if ((useHttps && webPort != 443) || (!useHttps && webPort != 80)) {
            result.append(String.format(":%d", webPort));
        }
        result.append("/");
        return result.toString();
    }

    public String getUrl(String pathAndQuery) {
        if ((pathAndQuery.length() > 0 && pathAndQuery.charAt(0) == '/')) {
            return getUrl() + pathAndQuery.substring(1);
        } else {
            return getUrl() + pathAndQuery;
        }
    }

    public String getSshUrl() {
        return String.format("ssh://%s@%s:%d", sshUser, host, sshPort);
    }

    /**
     * Fetch from Gerrit REST API and return as a string containing valid JSON.
     *
     * @param pathAndQuery The path and query part of the URL, others come from current object
     * @return a string containing valid JSON by removing leading 4 dummy chars.
     * @throws IOException If any error occurs (connection error, read error)
     */
    public String fetch(String pathAndQuery) throws IOException {
        return Fetcher.fetch(new URL(getUrl(pathAndQuery))).substring(4);
    }

    public static class Builder {

        private final String host;

        private int sshPort = 29418;
        private String sshUser;
        private String sshIdentityFile;
        private Integer webPort;
        private boolean useHttps = true;
        private boolean dryRun = false;

        public Builder(String host) {
            this.host = host;
        }

        public Gerrit build() {
            updateWebPort();
            return new Gerrit(host, sshPort, sshUser, sshIdentityFile, webPort, useHttps, dryRun);
        }

        private void updateWebPort() {
            if (webPort != null)
                return;
            webPort = useHttps ? 443 : 80;
        }

        public Builder sshPort(int sshPort) {
            this.sshPort = sshPort;
            return this;
        }

        public Builder sshUser(String sshUser) {
            this.sshUser = sshUser;
            return this;
        }

        public Builder sshIdentityFile(String sshIdentityFile) {
            this.sshIdentityFile = sshIdentityFile;
            return this;
        }

        public Builder webPort(int webPort) {
            this.webPort = webPort;
            return this;
        }

        public Builder useHttps(boolean useHttps) {
            this.useHttps = useHttps;
            updateWebPort();
            return this;
        }

        public Builder dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }
    }
}
