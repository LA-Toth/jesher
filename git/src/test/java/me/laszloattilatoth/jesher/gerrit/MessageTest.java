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

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class MessageTest {

    @Test
    void constructorsAndGet() {
        assertThat(new Message().get(), is(""));
        assertThat(new Message("Hello World").get(), is("Hello World"));
    }

    @Test
    void add() {
        assertThat(new Message().add("Hello").get(), is("Hello"));
        assertThat(new Message("Hello ").add(" World ").get(), is("Hello  World "));
        assertThat(new Message("Hello ").add(" World ").add("Championship").get(), is("Hello  World Championship"));
    }

    @Test
    void addBlockToEmptyMessage() {
        assertThat(new Message().addBlock("Hello\nx").get(), is("Hello\nx"));
    }

    @Test
    void addBlockToNonEmptyMessage() {
        assertThat(new Message().addBlock("Hello\nx").addBlock(" World ").get(), is("Hello\nx\n\n World "));
        assertThat(new Message("Hello\nWorld").addBlock("Multi\nLine\nText").get(),
                is("Hello\nWorld\n\nMulti\nLine\nText"));
    }
}
