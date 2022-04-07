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

import java.util.ArrayList;
import java.util.List;

public class ApacheHiveFilenameMapper implements FilenameMapper {
    private static final String[] METASTORE_SUBDIRS = {
            "client", "server", "common"
    };

    @Override
    public List<String> map(String filename) {
        assert !filename.endsWith("/");
        List<String> result = new ArrayList<>();
        result.add(filename);
        if (filename.startsWith("standalone-metastore/")) {
            for (String subdir : METASTORE_SUBDIRS) {
                result.add(String.format("standalone-metastore/metastore-%s/%s", subdir, filename.split("/", 2)[1]));
            }
        }
        if (filename.equals("standalone-metastore/src/main/java/org/apache/hadoop/hive/metastore/HiveMetaStore.java")) {
            result.add("standalone-metastore/metastore-server/src/main/java/org/apache/hadoop/hive/metastore/HMSHandler.java");
        }
        return result;
    }
}
