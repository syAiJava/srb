package com.atguigu.srb.core.service.impl;

import com.alibaba.excel.EasyExcel;
import com.atguigu.srb.core.listener.ExcelDictDTOListener;
import com.atguigu.srb.core.pojo.entity.Dict;
import com.atguigu.srb.core.mapper.DictMapper;
import com.atguigu.srb.core.pojo.entity.dto.ExcelDictDTO;
import com.atguigu.srb.core.service.DictService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 数据字典 服务实现类
 * </p>
 *
 * @author sy
 * @since 2022-12-28
 */
@Service
@Slf4j
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {


    @Resource
    private RedisTemplate redisTemplate;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void importData(InputStream inputStream) {
        EasyExcel.read(inputStream, ExcelDictDTO.class,new ExcelDictDTOListener(baseMapper)).sheet().doRead();
        log.info("Excel导入成功");
    }

    @Override
    public List<ExcelDictDTO> listDictData() {
        List<Dict> dictList = baseMapper.selectList(null);
        List<ExcelDictDTO> excelDictDTOList = new ArrayList<>(dictList.size());
        dictList.forEach(dict -> {
            ExcelDictDTO excelDictDTO = new ExcelDictDTO();
            BeanUtils.copyProperties(dict,excelDictDTO);
            excelDictDTOList.add(excelDictDTO);

        });
        return excelDictDTOList;
    }

    @Override
    public List<Dict> listByParentId(Long parentId) {
        try {
            List<Dict>  dicts= (List<Dict>) redisTemplate.opsForValue().get("srb:core:dictList:" + parentId);
            if(dicts!=null){
                log.info("从redis中直接获取数据");
                return dicts;
            }
        } catch (Exception e) {
            log.error("redis服务器异常:"+ ExceptionUtils.getStackTrace(e));
        }
        QueryWrapper<Dict> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pa" +
                "rent_id",parentId);
        List<Dict> dictList = baseMapper.selectList(queryWrapper);
        dictList.forEach(dict -> {
            boolean hasChildren=this.hasChildren(dict.getId());
            dict.setHasChildren(hasChildren);

        });

        try {
            redisTemplate.opsForValue().set("srb:core:dictList:"+parentId,dictList,5, TimeUnit.MINUTES);
            log.info("数据存入redis");
        } catch (Exception e) {
            log.error("数据存入redis");
        }
        return dictList;
    }

    private boolean hasChildren(Long id){
        QueryWrapper<Dict> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id",id);
        Integer count = baseMapper.selectCount(queryWrapper);
        if(count.intValue()>0){
            return true;
        }else{
            return false;
        }

    }
}
