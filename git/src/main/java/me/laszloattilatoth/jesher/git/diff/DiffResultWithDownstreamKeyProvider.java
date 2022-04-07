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

/**
 * Provides the keys for the DiffResult's toMap method with 'downstream_only' instead of 'local_only'.
 *
 * @see DiffResultKeyProvider for further details.
 */
public class DiffResultWithDownstreamKeyProvider implements DiffResultKeyProvider {

    @Override
    public String localOnly() {
        return "downstream_only";
    }
}
