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

package me.laszloattilatoth.jesher.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ProcessHelper {

    private ProcessHelper() {}

    public static BufferedReader getOutputReader(File directory, String... args) throws IOException {
        //System.out.println(directory);
        //System.out.println(String.join(" ", Arrays.stream(args).toList()));
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        if (directory != null)
            pb.directory(directory);
        Process p = pb.start();
        return p.inputReader();
    }

    public static int run(File directory, String... args) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        if (directory != null)
            pb.directory(directory);
        Process p = pb.start();
        return p.waitFor();
    }

    public static int runWithoutOutput(File directory, String... args) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        if (directory != null)
            pb.directory(directory);
        Process p = pb.start();
        return p.waitFor();
    }

    public static InputStream getOutputStream(File directory, String... args) throws IOException {
        //System.out.println(String.join(" ", Arrays.stream(args).toList()));
        ProcessBuilder pb = new ProcessBuilder(args)
                .inheritIO();
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        if (directory != null)
            pb.directory(directory);
        Process p = pb.start();
        try {
            int i = p.waitFor();
            System.out.printf("GOS WF %d%n", i);
            return p.getInputStream();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getOutputLine(File directory, String... args) throws IOException {
        return getOutputReader(directory, args).readLine();
    }
}
