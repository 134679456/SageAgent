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

import com.nageoffer.ai.ragent.framework.convention.ChatMessage;
import com.nageoffer.ai.ragent.infra.chat.LLMService;
import com.nageoffer.ai.ragent.infra.chat.StreamCallback;
import com.nageoffer.ai.ragent.rag.core.guidance.IntentGuidanceService;
import com.nageoffer.ai.ragent.rag.core.intent.IntentResolver;
import com.nageoffer.ai.ragent.rag.core.memory.ConversationMemoryService;
import com.nageoffer.ai.ragent.rag.core.prompt.PromptTemplateLoader;
import com.nageoffer.ai.ragent.rag.core.prompt.RAGPromptService;
import com.nageoffer.ai.ragent.rag.core.retrieve.RetrievalEngine;
import com.nageoffer.ai.ragent.rag.core.rewrite.QueryRewriteService;
import com.nageoffer.ai.ragent.rag.service.handler.StreamTaskManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamChatPipelineTest {

    @Mock
    private ConversationMemoryService memoryService;
    @Mock
    private ConversationHistoryQuestionService conversationHistoryQuestionService;
    @Mock
    private QueryRewriteService queryRewriteService;
    @Mock
    private IntentResolver intentResolver;
    @Mock
    private IntentGuidanceService guidanceService;
    @Mock
    private RetrievalEngine retrievalEngine;
    @Mock
    private LLMService llmService;
    @Mock
    private RAGPromptService promptBuilder;
    @Mock
    private PromptTemplateLoader promptTemplateLoader;
    @Mock
    private StreamTaskManager taskManager;
    @Mock
    private StreamCallback callback;

    private StreamChatPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new StreamChatPipeline(
                memoryService,
                conversationHistoryQuestionService,
                queryRewriteService,
                intentResolver,
                guidanceService,
                retrievalEngine,
                llmService,
                promptBuilder,
                promptTemplateLoader,
                taskManager
        );
    }

    @Test
    void shouldShortCircuitConversationHistoryQuestionBeforeRewrite() {
        String question = "我前面问过的所有问题有哪些？";
        when(memoryService.loadAndAppend(eq("c1"), eq("u1"), any(ChatMessage.class)))
                .thenReturn(List.of(ChatMessage.user("腾讯科技的工单进展如何？")));
        when(conversationHistoryQuestionService.answerIfSupported(question, "c1", "u1"))
                .thenReturn(Optional.of("在当前问题之前，你问过 1 个问题：\n1. 腾讯科技的工单进展如何？"));

        StreamChatContext ctx = StreamChatContext.builder()
                .question(question)
                .conversationId("c1")
                .taskId("t1")
                .userId("u1")
                .callback(callback)
                .build();

        pipeline.execute(ctx);

        verify(callback).onContent("在当前问题之前，你问过 1 个问题：\n1. 腾讯科技的工单进展如何？");
        verify(callback).onComplete();
        verifyNoInteractions(queryRewriteService, intentResolver, guidanceService, retrievalEngine, llmService, promptBuilder, promptTemplateLoader, taskManager);
    }
}
