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

public class Message {
    private final StringBuilder message = new StringBuilder();

    public Message() {}

    public Message(String message) {
        this.message.append(message);
    }

    public Message add(String message) {
        this.message.append(message);
        return this;
    }

    public Message addBlock(String message) {
        if (this.message.length() > 0) {
            this.message.append("\n\n");
        }
        add(message);
        return this;
    }

    public String get() {
        return message.toString();
    }
}
