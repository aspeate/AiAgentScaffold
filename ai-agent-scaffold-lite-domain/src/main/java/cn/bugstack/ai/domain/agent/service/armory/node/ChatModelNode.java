package cn.bugstack.ai.domain.agent.service.armory.node;

import cn.bugstack.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.bugstack.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.bugstack.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.bugstack.ai.domain.agent.service.armory.AbstractArmorySupport;
import cn.bugstack.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChatModelNode extends AbstractArmorySupport {
    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 AiApiNode");
        //从上下文中获取openAiApi对象
        OpenAiApi openAiApi = dynamicContext.getOpenAiApi();
        //从yml文件中获取配置
        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        AiAgentConfigTableVO.Module.ChatModel chatModelConfig = aiAgentConfigTableVO.getModule().getChatModel();
        //把从yml文件中获取的配置ToolMcp转换成McpSyncClient
        List<McpSyncClient> mcpSyncClients = new ArrayList<>();
        List<AiAgentConfigTableVO.Module.ChatModel.ToolMcp> toolMcpList = chatModelConfig.getToolMcpList();
        for (AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp : toolMcpList) {
            mcpSyncClients.add(createMcpSyncClient(toolMcp));
        }
        //创建ChatModel对象
        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(chatModelConfig.getModel())
                        .toolCallbacks(SyncMcpToolCallbackProvider.builder()
                                .mcpClients(mcpSyncClients).build()
                                .getToolCallbacks())
                        .build())
                .build();
        //把ChatModel对象放入上下文中
        dynamicContext.setChatModel(chatModel);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }

    private McpSyncClient createMcpSyncClient(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) throws Exception {
        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.SSEServerParameters sseConfig = toolMcp.getSse();
        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters stdioConfig = toolMcp.getStdio();

        if (sseConfig != null) {
            String originalBaseUri = sseConfig.getBaseUri();
            String baseUri = originalBaseUri;
            String sseEndpoint = sseConfig.getSseEndpoint();

            if (StringUtils.isBlank(sseEndpoint)) {
                //当sseEndpoint为空时，从baseUri中获取
                URL url = new URL(originalBaseUri);//"http://127.0.0.1:9999/sse?apiKey=xxxx"

                String protocol = url.getProtocol();// http
                String host = url.getHost();// 127.0.0.1
                int port = url.getPort();// 9999

                String baseUrl = port == -1 ? protocol + "://" + host : protocol + "://" + host + ":" + port;// http://127.0.0.1:9999

                int index = originalBaseUri.indexOf(baseUrl);// "http://127.0.0.1:9999/sse?apiKey=xxxx"
                                                             // http://127.0.0.1:9999          结果index = 0
                if (index != -1) {
                    //截取baseUrl得到sseEndpoint
                    sseEndpoint = originalBaseUri.substring(index + baseUrl.length());// /sse?apiKey=xxxx
                }
                baseUri = baseUrl;
            }

            //默认sseEndpoint为/sse
            sseEndpoint = StringUtils.isBlank(sseEndpoint) ? "/sse" : sseEndpoint;
            //创建HttpClientSseClientTransport对象
            HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport.builder(baseUri)
                    .sseEndpoint(sseEndpoint)
                    .build();
            //创建McpSyncClient对象
            McpSyncClient mcpSyncClient = McpClient
                    .sync(sseClientTransport)
                    .requestTimeout(Duration.ofMinutes(360))
                    .build();
            //初始化
            McpSchema.InitializeResult initialize = mcpSyncClient.initialize();

            log.info("tool sse mcp initialize {}", initialize);

            return mcpSyncClient;

        }
        if (null != stdioConfig) {
            AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters.ServerParameters serverParameters = stdioConfig.getServerParameters();

            ServerParameters stdioParams = ServerParameters.builder(serverParameters.getCommand())
                    .args(serverParameters.getArgs())
                    .env(serverParameters.getEnv())
                    .build();

            McpSyncClient mcpSyncClient = McpClient.sync(new StdioClientTransport(stdioParams, new JacksonMcpJsonMapper(new ObjectMapper())))
                    .requestTimeout(Duration.ofSeconds(stdioConfig.getRequestTimeout())).build();

            McpSchema.InitializeResult initialize = mcpSyncClient.initialize();

            log.info("tool stdio mcp initialize {}", initialize);

            return mcpSyncClient;
        }

        throw new RuntimeException("tool mcp sse and stdio is null!");


    }
}