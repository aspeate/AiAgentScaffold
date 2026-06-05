package cn.bugstack.ai.domain.agent.service.armory.factory;

import lombok.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DefaultArmoryFactory {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DynamicContext{
        private Map<String, Object> dataObjects = new HashMap<>();

        public <T> void setValue(String key, T value){
            dataObjects.put(key, value);
        }
        public <T> T getValue(String key){
            return (T) dataObjects.get(key);
        }
    }
}
