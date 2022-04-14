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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record DiffResult(String localCommitId, String upstreamCommitId,
                         List<String> same, List<String> different,
                         List<String> localOnly, List<String> upstreamOnly) {
    public Map<String, List<String>> toMap() {
        return toMap(new DiffResultKeyProvider() {
        });
    }

    public Map<String, List<String>> toMap(DiffResultKeyProvider keyProvider) {
        Map<String, List<String>> result = new HashMap<>();
        result.put(keyProvider.same(), new ArrayList<>(same));
        result.put(keyProvider.different(), new ArrayList<>(different));
        result.put(keyProvider.localOnly(), new ArrayList<>(localOnly));
        result.put(keyProvider.upstreamOnly(), new ArrayList<>(upstreamOnly));
        return result;
    }

    public boolean isSameAsUpstream() {
        return !same.isEmpty() && different.isEmpty() && localOnly.isEmpty() && upstreamOnly.isEmpty();
    }
}
