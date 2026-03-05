package com.speakforyou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakforyou.model.entity.ChatSessionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSessionEntity> {
}
