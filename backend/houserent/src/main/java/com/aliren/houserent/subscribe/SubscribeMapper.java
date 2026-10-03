package com.aliren.houserent.subscribe;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SubscribeMapper extends BaseMapper<Subscribe> {

    /** 推送计数 +1（订阅匹配落库时调用，保持 push_count 与推送历史一致） */
    @Update("UPDATE subscribe SET push_count = push_count + 1 WHERE id = #{id}")
    int incrementPushCount(@Param("id") Long id);
}
