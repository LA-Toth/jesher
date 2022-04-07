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

package me.laszloattilatoth.jesher.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Convert {
    private Convert() {}

    public static String toYaml(Object o) {
        Yaml yaml = new Yaml();
        return yaml.dump(o);
    }

    public static String toJson(Object o) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mapper.writeValue(output, o);
        return output.toString(StandardCharsets.UTF_8);
    }
}
