package com.aliren.core.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    default User selectByDingtalkUserId(String dingtalkUserId) {
        return selectOne(new QueryWrapper<User>().eq("dingtalk_userid", dingtalkUserId));
    }
}
