/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nageoffer.ai.ragent.rag.service.pipeline;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.nageoffer.ai.ragent.rag.controller.vo.ConversationMessageVO;
import com.nageoffer.ai.ragent.rag.enums.ConversationMessageOrder;
import com.nageoffer.ai.ragent.rag.service.ConversationMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ConversationHistoryQuestionService {

    private static final List<Pattern> LIST_PATTERNS = List.of(
            Pattern.compile(".*(前面|之前|此前|到目前为止|到现在|历史|刚才|刚刚).*(问过|问了|提过|提了).*(所有|全部|哪些|什么|啥).*"),
            Pattern.compile(".*(我都问了什么|我问过哪些问题|我前面问过的所有问题有哪些|之前问过的问题有哪些|前面问过的问题有哪些).*")
    );
    private static final List<Pattern> LAST_PATTERNS = List.of(
            Pattern.compile(".*(刚才|刚刚|上一个|前一个|上一条|前面一次).*(问了什么|问的什么|提了什么|说了什么|问题).*"),
            Pattern.compile(".*(刚才我问了什么问题|我刚才问了什么问题|刚才我问了什么|我刚才问了什么).*")
    );
    private static final List<Pattern> COUNT_PATTERNS = List.of(
            Pattern.compile(".*(一共|总共|到目前为止|到现在).*(问过|问了|提过|提了).*(多少|几个|几次|问题).*"),
            Pattern.compile(".*(问过|问了|提过|提了).*(多少|几个|几次).*(问题|次).*")
    );

    private final ConversationMessageService conversationMessageService;

    public Optional<String> answerIfSupported(String question, String conversationId, String userId) {
        HistoryQuestionType questionType = detect(question);
        if (questionType == HistoryQuestionType.NONE) {
            return Optional.empty();
        }

        List<String> previousQuestions = loadPreviousQuestions(conversationId, userId, question);
        return Optional.of(formatAnswer(questionType, previousQuestions));
    }

    private HistoryQuestionType detect(String question) {
        String normalized = normalize(question);
        if (StrUtil.isBlank(normalized)) {
            return HistoryQuestionType.NONE;
        }
        if (matchesAny(normalized, LAST_PATTERNS)) {
            return HistoryQuestionType.LAST_ONE;
        }
        if (matchesAny(normalized, COUNT_PATTERNS)) {
            return HistoryQuestionType.COUNT_ALL;
        }
        if (matchesAny(normalized, LIST_PATTERNS)) {
            return HistoryQuestionType.LIST_ALL;
        }
        return HistoryQuestionType.NONE;
    }

    private List<String> loadPreviousQuestions(String conversationId, String userId, String currentQuestion) {
        if (StrUtil.isBlank(conversationId) || StrUtil.isBlank(userId)) {
            return List.of();
        }

        List<ConversationMessageVO> messages = conversationMessageService.listMessages(
                conversationId,
                userId,
                null,
                ConversationMessageOrder.ASC
        );
        if (CollUtil.isEmpty(messages)) {
            return List.of();
        }

        List<String> userQuestions = new ArrayList<>();
        for (ConversationMessageVO message : messages) {
            if (message == null
                    || !"user".equalsIgnoreCase(message.getRole())
                    || StrUtil.isBlank(message.getContent())) {
                continue;
            }
            userQuestions.add(message.getContent().trim());
        }

        if (CollUtil.isEmpty(userQuestions)) {
            return List.of();
        }

        String normalizedCurrentQuestion = normalize(currentQuestion);
        int lastIndex = userQuestions.size() - 1;
        if (StrUtil.equals(normalize(userQuestions.get(lastIndex)), normalizedCurrentQuestion)) {
            userQuestions.remove(lastIndex);
        }
        return userQuestions;
    }

    private String formatAnswer(HistoryQuestionType questionType, List<String> previousQuestions) {
        if (CollUtil.isEmpty(previousQuestions)) {
            return "在当前问题之前，你还没有问过其他问题。";
        }

        return switch (questionType) {
            case LAST_ONE -> "你刚才问的是：" + previousQuestions.get(previousQuestions.size() - 1);
            case COUNT_ALL -> "在当前问题之前，你一共问过 " + previousQuestions.size() + " 个问题。";
            case LIST_ALL -> buildListAnswer(previousQuestions);
            case NONE -> "";
        };
    }

    private String buildListAnswer(List<String> previousQuestions) {
        StringBuilder answer = new StringBuilder("在当前问题之前，你问过 ")
                .append(previousQuestions.size())
                .append(" 个问题：");
        for (int i = 0; i < previousQuestions.size(); i++) {
            answer.append('\n')
                    .append(i + 1)
                    .append(". ")
                    .append(previousQuestions.get(i));
        }
        return answer.toString();
    }

    private boolean matchesAny(String question, List<Pattern> patterns) {
        return patterns.stream().anyMatch(pattern -> pattern.matcher(question).matches());
    }

    private String normalize(String question) {
        if (question == null) {
            return "";
        }
        return question.replaceAll("\\s+", "")
                .replace('？', '?')
                .replace('！', '!')
                .replace('，', ',')
                .replace('。', '.')
                .replace('：', ':')
                .trim();
    }

    private enum HistoryQuestionType {
        NONE,
        LAST_ONE,
        LIST_ALL,
        COUNT_ALL
    }
}
