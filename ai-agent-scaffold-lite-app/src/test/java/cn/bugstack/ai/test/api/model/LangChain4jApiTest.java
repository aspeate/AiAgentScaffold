package cn.bugstack.ai.test.api.model;

import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class LangChain4jApiTest {

    public static void main(String[] args) {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey("sk-9a0da2ca434742d9ae14066cf0e3ba96")
                .modelName("deepseek-chat")
                .build();

        String chat = model.chat("hi 你好哇!");
        log.info("测试结果:{}", chat);
    }

}
