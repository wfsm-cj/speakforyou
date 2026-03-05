package com.speakforyou.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.speakforyou.mapper.PersonaMapper;
import com.speakforyou.common.BizException;
import com.speakforyou.model.dto.PersonaDtos;
import com.speakforyou.model.entity.PersonaEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PersonaService {

    private final PersonaMapper personaMapper;

    public PersonaService(PersonaMapper personaMapper) {
        this.personaMapper = personaMapper;
    }

    public List<PersonaDtos.PersonaView> list(Long userId) {
        return personaMapper.selectList(new LambdaQueryWrapper<PersonaEntity>()
                        .and(w -> w.eq(PersonaEntity::isSystem, true).or().eq(PersonaEntity::getUserId, userId))
                        .orderByAsc(PersonaEntity::getId))
                .stream()
                .map(this::toView)
                .toList();
    }

    public PersonaEntity getPersonaForUser(Long userId, Long personaId) {
        PersonaEntity persona = personaMapper.selectById(personaId);
        if (persona == null) {
            throw new BizException(4004, "人格不存在");
        }
        if (!persona.isSystem() && !userId.equals(persona.getUserId())) {
            throw new BizException(4004, "人格不存在");
        }
        return persona;
    }

    @Transactional
    public PersonaDtos.PersonaView create(Long userId, PersonaDtos.CreatePersonaRequest request) {
        PersonaEntity entity = new PersonaEntity();
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setTone(request.tone());
        entity.setSystem(false);
        entity.setUserId(userId);
        entity.setCreatedAt(LocalDateTime.now());
        personaMapper.insert(entity);
        return toView(entity);
    }

    @Transactional
    public PersonaDtos.PersonaView update(Long userId, Long personaId, PersonaDtos.CreatePersonaRequest request) {
        PersonaEntity entity = getPersonaForUser(userId, personaId);
        if (entity.isSystem()) {
            throw new BizException(4001, "系统人格不允许编辑");
        }
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setTone(request.tone());
        personaMapper.updateById(entity);
        return toView(entity);
    }

    @Transactional
    public void delete(Long userId, Long personaId) {
        PersonaEntity entity = getPersonaForUser(userId, personaId);
        if (entity.isSystem()) {
            throw new BizException(4001, "系统人格不允许删除");
        }
        personaMapper.deleteById(entity.getId());
    }

    private PersonaDtos.PersonaView toView(PersonaEntity entity) {
        return new PersonaDtos.PersonaView(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getTone(),
                entity.isSystem()
        );
    }
}
