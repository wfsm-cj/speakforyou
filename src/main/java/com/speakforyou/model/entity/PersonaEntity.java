package com.speakforyou.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("persona")
public class PersonaEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String tone;

    @TableField("is_system")
    private boolean isSystem;

    @TableField("user_id")
    private Long userId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
