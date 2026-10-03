package com.aliren.houserent.pushlog;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PushLogMapper extends BaseMapper<PushLog> {

    /** 某订阅当天（自然日）推送条数：防骚扰每日上限判断 */
    @Select("SELECT COUNT(*) FROM push_log WHERE subscribe_id = #{subscribeId} "
            + "AND created_at >= #{dayStart} AND created_at < #{dayEnd}")
    long countTodayBySubscribe(@Param("subscribeId") Long subscribeId,
                               @Param("dayStart") java.time.LocalDateTime dayStart,
                               @Param("dayEnd") java.time.LocalDateTime dayEnd);
}
