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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class GerritTest {
    Gerrit gerrit = new Gerrit.Builder("gerrit.example.com").build();

    @Test
    void host() {
        assertThat(gerrit.host(), is("gerrit.example.com"));
    }

    @Test
    void getUrlWithDefaultSetup() {
        assertThat(gerrit.getUrl(), is("https://gerrit.example.com/"));
        assertThat(gerrit.getUrl(""), is("https://gerrit.example.com/"));
        assertThat(gerrit.getUrl("/"), is("https://gerrit.example.com/"));
        assertThat(gerrit.getUrl("/something?value"), is("https://gerrit.example.com/something?value"));
        assertThat(gerrit.getUrl("something?value"), is("https://gerrit.example.com/something?value"));
    }

    @Test
    void getUrlWithDifferentPorts() {
        assertThat(new Gerrit.Builder("x.com").build().getUrl(), is("https://x.com/"));
        assertThat(new Gerrit.Builder("x.com").webPort(443).build().getUrl(), is("https://x.com/"));
        assertThat(new Gerrit.Builder("x.com").webPort(4444).build().getUrl(), is("https://x.com:4444/"));
        assertThat(new Gerrit.Builder("x.com").useHttps(false).build().getUrl(), is("http://x.com/"));
        assertThat(new Gerrit.Builder("x.com").useHttps(false).webPort(80).build().getUrl(), is("http://x.com/"));
        assertThat(new Gerrit.Builder("x.com").useHttps(false).webPort(443).build().getUrl(), is("http://x.com:443/"));
        assertThat(new Gerrit.Builder("x.com").useHttps(false).webPort(443).build().getUrl("/path"), is("http://x.com:443/path"));
    }
}
