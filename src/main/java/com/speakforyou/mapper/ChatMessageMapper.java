package com.speakforyou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakforyou.model.entity.ChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageEntity> {
}
