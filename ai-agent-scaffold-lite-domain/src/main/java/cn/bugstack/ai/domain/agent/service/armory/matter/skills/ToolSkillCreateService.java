package cn.bugstack.ai.domain.agent.service.armory.matter.skills;

import cn.bugstack.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import org.springframework.ai.tool.ToolCallback;

public interface ToolSkillCreateService {

    ToolCallback[] buildCallback(AiAgentConfigTableVO.Module.ChatModel.ToolSkills toolSkills) throws Exception;


}
