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

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

class ReviewMessageTest {
    ReviewMessage m = new ReviewMessage();

    private void assertThatCr(int value) {
        assertThat(m.codeReview(), is(value));
        assertThat(m.getLabel("Code-Review"), is(value));
    }

    @Test
    void testEmptyMessage() {
        assertThat(m.get(), is(""));
        assertThatCr(0);
        assertThat(m.hasCodeReview(), is(false));
        assertThat(m.hasAdditionalLables(), is(false));
        assertThat(m.hasLabels(), is(false));
        assertThat(m.getLabels().isEmpty(), is(true));
    }

    @Test
    void addBlockWithReview() {
        assertThat(m.addBlockWithReview("some text\nnew line", 1), is(m));
        assertThatCr(1);
        assertThat(m.get(), is(" :) some text\nnew line"));
        assertThat(m.hasCodeReview(), is(true));
        assertThat(m.hasAdditionalLables(), is(false));
        assertThat(m.hasLabels(), is(true));
        assertThat(m.getLabels(), is(Map.of("Code-Review", 1)));
    }

    @Test
    void addBlocksWithDifferentCodeReviewValues() {
        assertThat(m.addBlockWithReview("some text\nnew line", 1), is(m));
        m.addBlockWithReview("another\nblock", -1);
        assertThat(m.get(), is(" :) some text\nnew line\n\n/o\\ another\nblock"));
        assertThat(m.getLabels(), is(Map.of("Code-Review", -1)));
        m.addBlockWithReview("third", 0);
        assertThat(m.get(), is(" :) some text\nnew line\n\n/o\\ another\nblock\n\n    third"));
        assertThatCr(-1);
    }

    @Test
    void testCodeReviewValueComparisons() {
        // Priority order: 0 < 1 < -1 < +2 < -2
        assertThat(m.codeReview(), is(0));
        assertCr(1, 1);
        assertCr(0, 1);
        assertCr(-1, -1);
        assertCr(1, -1);
        assertCr(2, 2);
        assertCr(-1, 2);
        assertCr(-2, -2);
        assertCr(2, -2);
    }

    private void assertCr(int valueToSet, int expectedValue) {
        m.addBlockWithReview("x", valueToSet);
        assertThatCr(expectedValue);
    }

    @Test
    void testLabelComparisons() {
        assertLabelComparison("Code-Review");
        assertLabelComparison("Whatever-Else");
    }

    private void assertLabelComparison(String label) {
        // Similarly to CR, priority order (no check if the value is valid): 0 < 1 < -1 < +2 < -2
        assertLabel(label, 1, 1);
        assertLabel(label, 0, 1);
        assertLabel(label, -1, -1);
        assertLabel(label, 1, -1);
        assertLabel(label, 2, 2);
        assertLabel(label, -1, 2);
        assertLabel(label, -2, -2);
        assertLabel(label, 2, -2);
    }

    private void assertLabel(String label, int valueToSet, int expectedValue) {
        m.addLabel(label, valueToSet);
        assertThat(m.getLabel(label), is(expectedValue));
    }

    @Test
    void testAddingExtraLabel() {
        m.addLabel("Some-Label", -1);
        assertThat(m.hasCodeReview(), is(false));
        assertThat(m.hasAdditionalLables(), is(true));
        assertThat(m.hasLabels(), is(true));
        assertThat(m.getLabels(), is(Map.of("Some-Label", -1)));
        assertThatCr(0);
        assertThat(m.get(), is(""));
    }

    @Test
    void testAddCodeReviewAsExtraLabel() {
        m.addLabel("Code-Review", -1);
        assertThat(m.hasCodeReview(), is(true));
        assertThat(m.hasAdditionalLables(), is(false));
        assertThat(m.hasLabels(), is(true));
        assertThat(m.getLabels(), is(Map.of("Code-Review", -1)));
        assertThat(m.codeReview(), is(-1));
        assertThat(m.get(), is(""));
    }

    @Test
    void getLabels() {
        m.addLabel("Code-Review", -1);
        m.addBlockWithReview("text", 1);
        m.addLabel("Something", 1);
        m.addLabel("Other", 12);
        assertThat(m.getLabels(), is(Map.of("Something", 1, "Other", 12, "Code-Review", -1)));
    }

    @Test
    void toGerritReview() {
        assertThat(m.toGerritReview(), is(Map.of("message", "")));
        assertThat(m.toGerritReview(true), is(Map.of("message", "")));

        m.addBlockWithReview("text", -1);
        m.addLabel("Something", 2);
        assertThat(m.toGerritReview(), is(Map.of("message", "/o\\ text", "labels", Map.of("Something", 2, "Code-Review", -1))));
        assertThat(m.toGerritReview(false), is(Map.of("message", "/o\\ text", "labels", Map.of("Something", 2, "Code-Review", -1))));
        assertThat(m.toGerritReview(true), is(Map.of("message", "/o\\ text")));
    }

    @Test
    void testPrivateComparableLabelValue() {
        // as a label: 0 < 1 < -1 < +2 < -2
        assertThat(m.comparableLabelValue(0), lessThan(m.comparableLabelValue(1)));
        assertThat(m.comparableLabelValue(1), lessThan(m.comparableLabelValue(-1)));
        assertThat(m.comparableLabelValue(-1), lessThan(m.comparableLabelValue(2)));
        assertThat(m.comparableLabelValue(2), lessThan(m.comparableLabelValue(-2)));
    }

    @Test
    void testCustomPrefix() {
        assertThat(new ReviewMessage(new CustomPrefix()).addBlockWithReview("txt", 0).get(), is("NTRL txt"));
        assertThat(new ReviewMessage(new CustomPrefix()).addBlockWithReview("txt", -3).get(), is(" BAD txt"));
        assertThat(new ReviewMessage(new CustomPrefix()).addBlockWithReview("txt", 2).get(), is("G00D txt"));
    }

    private static class CustomPrefix implements MessagePrefix {

        @Override
        public String good() {
            return "G00D ";
        }

        @Override
        public String neutral() {
            return "NTRL ";
        }

        @Override
        public String bad() {
            return " BAD ";
        }
    }
}
