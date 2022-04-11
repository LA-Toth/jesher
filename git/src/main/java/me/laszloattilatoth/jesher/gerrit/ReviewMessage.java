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

import java.util.HashMap;
import java.util.Map;

public class ReviewMessage extends Message {
    public static final String CODE_REVIEW_LABEL = "Code-Review";
    private int review;
    private final Map<String, Integer> labels = new HashMap<>();
    private final MessagePrefix messagePrefix;

    public ReviewMessage() {
        this(new DefaultMessagePrefix());
    }

    public ReviewMessage(MessagePrefix messagePrefix) {
        this.messagePrefix = messagePrefix;
    }

    public ReviewMessage addBlockWithReview(String message, int review) {
        addBlock(messagePrefix.byReviewValue(review) + message);
        if (shouldUpdateLabel(codeReview(), review))
            this.review = review;
        return this;
    }

    public ReviewMessage addLabel(String label, int value) {
        int previousValue = getLabel(label);
        if(!shouldUpdateLabel(previousValue, value))
            return this;
        if (label.equals(CODE_REVIEW_LABEL)) review = value;
        else labels.put(label, value);
        return this;
    }

    private boolean shouldUpdateLabel(int previousValue, int newValue) {
        // priority: 0 < 1 < -1 < +2 < -2
        return comparableLabelValue(previousValue) < comparableLabelValue(newValue);
    }

    int comparableLabelValue(int v) {
        return Math.abs(v) * 100 + (v < 0 ? 50 : 0);
    }

    public int codeReview() {
        return review;
    }

    public boolean hasCodeReview() {
        return review != 0;
    }

    public boolean hasAdditionalLables() {
        return !labels.isEmpty();
    }

    public boolean hasLabels() {
        return hasCodeReview() || hasAdditionalLables();
    }

    public int getLabel(String label) {
        if (label.equals(CODE_REVIEW_LABEL)) {
            return codeReview();
        } else {
            return labels.getOrDefault(label, 0);
        }
    }

    public Map<String, Integer> getLabels() {
        HashMap<String, Integer> result = new HashMap<>(labels);
        if (review != 0) {
            result.put(CODE_REVIEW_LABEL, review);
        }
        return result;
    }

    public Map<String, Object> toGerritReview() {
        return toGerritReview(false);
    }

    public Map<String, Object> toGerritReview(boolean messageOnly) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("message", get());
        if (!messageOnly && hasLabels()) {
            result.put("labels", getLabels());
        }

        return result;
    }
}
