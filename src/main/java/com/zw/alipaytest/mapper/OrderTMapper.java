package com.zw.alipaytest.mapper;

import com.zw.alipaytest.domain.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author zzw
 * @since 2023-04-08
 */
@Mapper
public interface OrderTMapper extends BaseMapper<Order> {

}
