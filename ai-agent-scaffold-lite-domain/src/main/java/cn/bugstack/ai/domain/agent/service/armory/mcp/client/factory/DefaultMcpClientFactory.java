package cn.bugstack.ai.domain.agent.service.armory.mcp.client.factory;

import cn.bugstack.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.bugstack.ai.domain.agent.service.armory.mcp.client.ToolMcpCreateService;
import cn.bugstack.ai.domain.agent.service.armory.mcp.client.impl.LocalToolMcpCreateService;
import cn.bugstack.ai.domain.agent.service.armory.mcp.client.impl.SSEToolMcpCreateService;
import cn.bugstack.ai.domain.agent.service.armory.mcp.client.impl.StdioToolMcpCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class DefaultMcpClientFactory {
    @Resource
    private SSEToolMcpCreateService sseToolMcpCreateService;

    @Resource
    private LocalToolMcpCreateService localToolMcpCreateService;

    @Resource
    private StdioToolMcpCreateService stdioToolMcpCreateService;

    public ToolMcpCreateService getToolMcpCreateService(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) {
        if (toolMcp.getSse() != null) return sseToolMcpCreateService;
        if (toolMcp.getLocal() != null) return localToolMcpCreateService;
        if (toolMcp.getStdio() != null) return stdioToolMcpCreateService;
        throw new RuntimeException("未找到对应的MCP服务");
    }


}
