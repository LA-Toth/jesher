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

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public record Commit(String commitId, String author, ZonedDateTime date,
                     String committer, ZonedDateTime commitDate,
                     String subject) {

    public Map<String, String> toMap() {
        Map<String, String> result = new HashMap<>();
        result.put("commit_id", commitId);
        result.put("author", author);
        result.put("date", date.toString());
        result.put("committer", committer);
        result.put("commitDate", commitDate.toString());
        result.put("subject", subject);
        return result;
    }
}