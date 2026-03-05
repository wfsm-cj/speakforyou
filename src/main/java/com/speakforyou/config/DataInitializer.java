//package com.speakforyou.config;
//
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.speakforyou.mapper.PersonaMapper;
//import com.speakforyou.mapper.SceneMapper;
//import com.speakforyou.mapper.UserMapper;
//import com.speakforyou.model.entity.PersonaEntity;
//import com.speakforyou.model.entity.SceneEntity;
//import com.speakforyou.model.entity.UserEntity;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Component
//public class DataInitializer implements CommandLineRunner {
//
//    private final UserMapper userMapper;
//    private final PersonaMapper personaMapper;
//    private final SceneMapper sceneMapper;
//    private final PasswordEncoder passwordEncoder;
//
//    public DataInitializer(
//            UserMapper userMapper,
//            PersonaMapper personaMapper,
//            SceneMapper sceneMapper,
//            PasswordEncoder passwordEncoder
//    ) {
//        this.userMapper = userMapper;
//        this.personaMapper = personaMapper;
//        this.sceneMapper = sceneMapper;
//        this.passwordEncoder = passwordEncoder;
//    }
//
//    @Override
//    public void run(String... args) {
//        initUsers();
//        initPersonas();
//        initScenes();
//    }
//
//    private void initUsers() {
//        Long count = userMapper.selectCount(new LambdaQueryWrapper<>());
//        if (count != null && count > 0) {
//            return;
//        }
//        UserEntity user = new UserEntity();
//        user.setUsername("alice");
//        user.setPassword(passwordEncoder.encode("123456"));
//        user.setNickname("小A");
//        user.setModelName("qwen-plus");
//        user.setRole("USER");
//        user.setCreatedAt(LocalDateTime.now());
//        user.setUpdatedAt(LocalDateTime.now());
//        userMapper.insert(user);
//    }
//
//    private void initPersonas() {
//        Long count = personaMapper.selectCount(new LambdaQueryWrapper<>());
//        if (count != null && count > 0) {
//            return;
//        }
//        List<PersonaEntity> personas = List.of(
//                persona("专业得体", "措辞严谨、礼貌周到、表达清晰", "用\"您\"、结构化表达"),
//                persona("轻松友好", "亲切随意、带点小幽默", "适当口语化，语气自然"),
//                persona("委婉拒绝", "圆滑婉转、不伤感情", "先肯定再转折，提供替代方案"),
//                persona("高情商化解", "化解矛盾、缓和气氛", "共情表达、积极引导"),
//                persona("简洁高效", "言简意赅、直奔主题", "短句直接，减少冗余")
//        );
//        personas.forEach(personaMapper::insert);
//    }
//
//    private PersonaEntity persona(String name, String desc, String tone) {
//        PersonaEntity p = new PersonaEntity();
//        p.setName(name);
//        p.setDescription(desc);
//        p.setTone(tone);
//        p.setSystem(true);
//        p.setCreatedAt(LocalDateTime.now());
//        return p;
//    }
//
//    private void initScenes() {
//        Long count = sceneMapper.selectCount(new LambdaQueryWrapper<>());
//        if (count != null && count > 0) {
//            return;
//        }
//        List<SceneEntity> scenes = List.of(
//                scene("同事日常聊天", "和平级同事的日常交流", "保持亲和力，适当延续话题"),
//                scene("领导/上级沟通", "和上司、领导的对话", "语气尊重、表达专业"),
//                scene("客户/合作方沟通", "商务往来、客户对接", "礼貌专业，措辞严谨"),
//                scene("委婉拒绝/推脱", "需要婉拒的各类场景", "先理解感谢，再说明困难并给替代方案")
//        );
//        scenes.forEach(sceneMapper::insert);
//    }
//
//    private SceneEntity scene(String name, String desc, String hint) {
//        SceneEntity s = new SceneEntity();
//        s.setName(name);
//        s.setDescription(desc);
//        s.setPromptHint(hint);
//        s.setSystem(true);
//        s.setCreatedAt(LocalDateTime.now());
//        return s;
//    }
//}
