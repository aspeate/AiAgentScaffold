package cn.bugstack.ai.domain.agent.service.armory.matter.mcp.client.impl;

import cn.bugstack.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.bugstack.ai.domain.agent.service.armory.matter.mcp.client.ToolMcpCreateService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;

@Slf4j
@Service
public class SSEToolMcpCreateService implements ToolMcpCreateService {
    @Override
    public ToolCallback[] buildCallback(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) throws Exception{
        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.SSEServerParameters sseConfig = toolMcp.getSse();

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

        return SyncMcpToolCallbackProvider.builder()
                .mcpClients(mcpSyncClient).build()
                .getToolCallbacks();

    }
}
