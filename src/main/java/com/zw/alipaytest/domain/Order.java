package com.zw.alipaytest.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 
 * </p>
 *
 * @author zzw
 * @since 2023-04-08
 */
@Getter
@Setter
@TableName("order_t")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("order_id")
    private String orderId;

    @TableField("name")
    private String name;

    @TableField("time")
    private Date time;

    @TableField("state")
    private String state;

    @TableField("total")
    private Double total;

    @TableField("delete_time")
    @TableLogic(value = "null",delval = "now()")
    private Date deleteTime;


}
