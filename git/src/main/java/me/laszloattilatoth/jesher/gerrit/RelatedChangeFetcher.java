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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelatedChangeFetcher {
    private final Gerrit gerrit;

    public RelatedChangeFetcher(Gerrit gerrit) {
        this.gerrit = gerrit;
    }

    public List<RelatedChange> fetch(int change, int revision) {
        try {
            Map<String, Object> data = Convert.fromJsonToMap(gerrit.fetch(String.format("changes/%d/revisions/%d/related", change, revision)));
            System.out.println(Convert.toJson(data));

            List<Map<String, Object>> changes = (List<Map<String, Object>>) data.get("changes");
            return changes.stream().map(c -> {
                Map<String, Object> commit = (Map<String, Object>) c.get("commit");
                return new RelatedChange(
                        (Integer) c.get("_change_number"),
                        (Integer) c.get("_change_number"),
                        (String) c.get("change_id"),
                        (Integer) c.get("_current_revision_number"),
                        (String) commit.get("commit"),
                        (String) ((List<Map<String, Object>>) commit.get("parents")).get(0).get("commit"),
                        (String) c.get("status")
                );
            }).toList();
        } catch (/* MalformedURLException | JsonProcessingException | */ IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<RelatedChange> fetchAsChain(int change, int revision) {
        List<RelatedChange> related = fetch(change, revision);
        if (related == null)
            return null;

        Map<String, String> commitToParent = new HashMap<>();
        Map<String, RelatedChange> commitToRelated = new HashMap<>();
        RelatedChange currentChange = null;

        for (RelatedChange c : related) {
            if (currentChange == null && c.changeNumber() == change && c.revision() == revision) {
                currentChange = c;
            }

            commitToParent.put(c.commitId(), c.parentCommitId());
            commitToRelated.put(c.commitId(), c);
        }

        List<RelatedChange> result = new ArrayList<>();
        while (currentChange != null) {
            result.add(currentChange);
            currentChange = commitToRelated.get(commitToParent.get(currentChange.commitId()));
        }

        return result;
    }
}
