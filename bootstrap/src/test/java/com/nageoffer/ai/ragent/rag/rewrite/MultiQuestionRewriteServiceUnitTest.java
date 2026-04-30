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

package com.nageoffer.ai.ragent.rag.rewrite;

import com.nageoffer.ai.ragent.infra.chat.LLMService;
import com.nageoffer.ai.ragent.framework.convention.ChatRequest;
import com.nageoffer.ai.ragent.rag.config.RAGConfigProperties;
import com.nageoffer.ai.ragent.rag.core.prompt.PromptTemplateLoader;
import com.nageoffer.ai.ragent.rag.core.rewrite.MultiQuestionRewriteService;
import com.nageoffer.ai.ragent.rag.core.rewrite.QueryTermMappingService;
import com.nageoffer.ai.ragent.rag.core.rewrite.RewriteResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiQuestionRewriteServiceUnitTest {

    @Mock
    private LLMService llmService;
    @Mock
    private QueryTermMappingService queryTermMappingService;
    @Mock
    private PromptTemplateLoader promptTemplateLoader;

    private MultiQuestionRewriteService service;

    @BeforeEach
    void setUp() {
        RAGConfigProperties properties = new RAGConfigProperties();
        properties.setQueryRewriteEnabled(true);
        service = new MultiQuestionRewriteService(
                llmService,
                properties,
                queryTermMappingService,
                promptTemplateLoader
        );
    }

    @Test
    void shouldParseJsonWrappedByExplanationText() {
        String question = "我前面问过的所有问题有哪些？";
        when(queryTermMappingService.normalize(question)).thenReturn(question);
        when(promptTemplateLoader.load(anyString())).thenReturn("system");
        when(llmService.chat(any(ChatRequest.class))).thenReturn("""
                结果如下：
                ```json
                {
                  "rewrite": "我前面问过的所有问题有哪些？",
                  "should_split": false,
                  "sub_questions": ["我前面问过的所有问题有哪些？"]
                }
                ```""");

        RewriteResult result = service.rewriteWithSplit(question, List.of());

        assertNotNull(result);
        assertEquals("我前面问过的所有问题有哪些？", result.rewrittenQuestion());
        assertEquals(List.of("我前面问过的所有问题有哪些？"), result.subQuestions());
    }
}
