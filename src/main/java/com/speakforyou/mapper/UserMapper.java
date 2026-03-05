package com.speakforyou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakforyou.model.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
