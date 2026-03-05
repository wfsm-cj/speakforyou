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
@TableName("scene")
public class SceneEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    @TableField("prompt_hint")
    private String promptHint;

    @TableField("is_system")
    private Boolean systemFlag = true;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
