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

import java.util.List;

public record RelatedChange(int changeNumber, int revision, String changeId, int latestRevision,
                            String commitId, String parentCommitId, String status) {

    public static final List<String> CLOSED_STATUSES = List.of("MERGED", "ABANDONED");

    boolean closed() {
        return CLOSED_STATUSES.contains(status);
    }
}
