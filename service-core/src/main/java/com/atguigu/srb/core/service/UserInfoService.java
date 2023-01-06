package com.atguigu.srb.core.service;

import com.atguigu.srb.core.pojo.entity.UserInfo;
import com.atguigu.srb.core.pojo.vo.RegisterVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户基本信息 服务类
 * </p>
 *
 * @author sy
 * @since 2022-12-28
 */
public interface UserInfoService extends IService<UserInfo> {

    void register(RegisterVO registerVO);
}
