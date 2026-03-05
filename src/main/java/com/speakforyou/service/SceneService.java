package com.speakforyou.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.speakforyou.mapper.SceneMapper;
import com.speakforyou.common.BizException;
import com.speakforyou.model.dto.SceneDtos;
import com.speakforyou.model.entity.SceneEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SceneService {

    private final SceneMapper sceneMapper;

    public SceneService(SceneMapper sceneMapper) {
        this.sceneMapper = sceneMapper;
    }

    public List<SceneDtos.SceneView> list() {
        return sceneMapper.selectList(new LambdaQueryWrapper<SceneEntity>()
                        .eq(SceneEntity::getSystemFlag, true)
                        .orderByAsc(SceneEntity::getId))
                .stream()
                .map(scene -> new SceneDtos.SceneView(scene.getId(), scene.getName(), scene.getDescription()))
                .toList();
    }

    public SceneEntity getById(Long id) {
        SceneEntity scene = sceneMapper.selectById(id);
        if (scene == null) {
            throw new BizException(4004, "场景不存在");
        }
        return scene;
    }
}
