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

import com.nageoffer.ai.ragent.rag.controller.vo.ConversationMessageVO;
import com.nageoffer.ai.ragent.rag.enums.ConversationMessageOrder;
import com.nageoffer.ai.ragent.rag.service.ConversationMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationHistoryQuestionServiceTest {

    @Mock
    private ConversationMessageService conversationMessageService;

    @Test
    void shouldListAllPreviousQuestionsWithoutCurrentQuestion() {
        ConversationHistoryQuestionService service = new ConversationHistoryQuestionService(conversationMessageService);
        when(conversationMessageService.listMessages("c1", "u1", null, ConversationMessageOrder.ASC))
                .thenReturn(List.of(
                        message("user", "腾讯科技的工单进展如何？"),
                        message("assistant", "这是回答"),
                        message("user", "刚才我问了什么问题"),
                        message("assistant", "这是回答"),
                        message("user", "我前面问过的所有问题有哪些？")
                ));

        Optional<String> answer = service.answerIfSupported("我前面问过的所有问题有哪些？", "c1", "u1");

        assertTrue(answer.isPresent());
        assertEquals("""
                在当前问题之前，你问过 2 个问题：
                1. 腾讯科技的工单进展如何？
                2. 刚才我问了什么问题""", answer.get());
    }

    @Test
    void shouldReturnLatestPreviousQuestion() {
        ConversationHistoryQuestionService service = new ConversationHistoryQuestionService(conversationMessageService);
        when(conversationMessageService.listMessages("c1", "u1", null, ConversationMessageOrder.ASC))
                .thenReturn(List.of(
                        message("user", "腾讯科技的工单进展如何？"),
                        message("assistant", "这是回答"),
                        message("user", "刚才我问了什么问题")
                ));

        Optional<String> answer = service.answerIfSupported("刚才我问了什么问题", "c1", "u1");

        assertTrue(answer.isPresent());
        assertEquals("你刚才问的是：腾讯科技的工单进展如何？", answer.get());
    }

    private ConversationMessageVO message(String role, String content) {
        return ConversationMessageVO.builder()
                .role(role)
                .content(content)
                .build();
    }
}
