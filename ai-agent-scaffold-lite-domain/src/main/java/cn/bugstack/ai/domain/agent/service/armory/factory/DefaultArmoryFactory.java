package cn.bugstack.ai.domain.agent.service.armory.factory;

import cn.bugstack.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.bugstack.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.bugstack.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.bugstack.ai.domain.agent.service.armory.node.RootNode;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.SequentialAgent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class DefaultArmoryFactory {
    @Resource
    private RootNode rootNode;

    public StrategyHandler<ArmoryCommandEntity, DynamicContext, AiAgentRegisterVO> armoryStrategyHandler() {
        return rootNode;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DynamicContext {
        /**
         * LLM API
         */
        private OpenAiApi openAiApi;
        /**
         * LLM ChatModel
         */
        private ChatModel chatModel;
        /**
         * 智能体配置组
         */
        private Map<String, BaseAgent> agentGroup = new HashMap<>();
        /**
         * 把它当做最后一个智能体节点
         */
        private SequentialAgent sequentialAgent;

        /**
         * 智能体Workflows
         */
        private List<AiAgentConfigTableVO.Module.ChatModel.AgentWorkflow> agentWorkflows = new ArrayList<>();


        private Map<String, Object> dataObjects = new HashMap<>();

        public <T> void setValue(String key, T value) {
            dataObjects.put(key, value);
        }

        public <T> T getValue(String key) {
            return (T) dataObjects.get(key);
        }

        public List<BaseAgent> getAgents(List<String> agentNames){
            if (agentNames == null || agentNames.isEmpty() || agentGroup == null) return Collections.emptyList();
            List<BaseAgent> agents = new ArrayList<>(agentNames.size());
            for (String agentName : agentNames) {
                BaseAgent agent = agentGroup.get(agentName);
                if (agent != null) agents.add(agent);
            }
            return agents;
        }
    }
}
