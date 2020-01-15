package com.atguigu.gmall.pms.service.impl;

        import com.atguigu.core.bean.PageVo;
        import com.atguigu.core.bean.Query;
        import com.atguigu.core.bean.QueryCondition;
        import com.atguigu.gmall.pms.dao.AttrAttrgroupRelationDao;
        import com.atguigu.gmall.pms.dao.AttrDao;
        import com.atguigu.gmall.pms.dao.AttrGroupDao;
        import com.atguigu.gmall.pms.dao.ProductAttrValueDao;
        import com.atguigu.gmall.pms.entity.AttrAttrgroupRelationEntity;
        import com.atguigu.gmall.pms.entity.AttrEntity;
        import com.atguigu.gmall.pms.entity.AttrGroupEntity;
        import com.atguigu.gmall.pms.entity.ProductAttrValueEntity;
        import com.atguigu.gmall.pms.service.AttrGroupService;
        import com.atguigu.gmall.pms.vo.GroupVo;
        import com.atguigu.gmall.pms.vo.ItemGroupVO;
        import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
        import com.baomidou.mybatisplus.core.metadata.IPage;
        import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
        import org.springframework.beans.BeanUtils;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.stereotype.Service;
        import org.springframework.util.CollectionUtils;

        import java.util.List;
        import java.util.stream.Collectors;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    private AttrAttrgroupRelationDao relationDao;

    @Autowired
    private AttrDao attrDao;

    @Autowired
    private ProductAttrValueDao attrValueDao;


    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public PageVo querGroupByCidPage(QueryCondition queryCondition, Long catId) {
        /**
         this.page()方法是从ServiceImpl继承来的，
         */

        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(queryCondition),
                new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catId)
        );
        return new PageVo(page);
    }

    /**
     * 查询组下的规格参数
     *
     * @param gid
     * @return
     */
    @Override
    public GroupVo queryGroupVoByGid(Long gid) {

        GroupVo groupVo = new GroupVo();
        //根据gid查询组
        AttrGroupEntity groupEntity = this.getById(gid);
        BeanUtils.copyProperties(groupEntity, groupVo);


        //查询中间表
        List<AttrAttrgroupRelationEntity> relationEntities = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", gid));
        groupVo.setRelations(relationEntities);

        //判断中间表是否为空
        if (CollectionUtils.isEmpty(relationEntities)) {
            return groupVo;
        }


        //获取所有规格参数的id
        List<Long> attrIds = relationEntities.stream().map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());


        //查询规格参数表
        List<AttrEntity> attrEntities = attrDao.selectBatchIds(attrIds);
        groupVo.setAttrEntities(attrEntities);

        return groupVo;
    }

    @Override
    public List<GroupVo> queryGroupVoByCid(long cid) {
        // 1.根据分类的id查询规格参数组
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", cid));

        //2.遍历规格参数组查询每个组下中间关系
        return groupEntities.stream().map(attrGroupEntity -> queryGroupVoByGid(attrGroupEntity.getAttrGroupId())).collect(Collectors.toList());

    }

    @Override
    public List<ItemGroupVO> queryItemGroupVOsByCidAndSpuId(Long cid, Long spuId) {

        //1.根据sku中的categoryId查询组
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", cid));
        if (CollectionUtils.isEmpty(groupEntities)) {
            return null;
        }


        return groupEntities.stream().map(groupEntitie -> {
            ItemGroupVO itemGroupVO = new ItemGroupVO();
            itemGroupVO.setId(groupEntitie.getAttrGroupId());
            itemGroupVO.setName(groupEntitie.getAttrGroupName());


            //2.遍历组到中间表查询每个组的规格参数id
            List<AttrAttrgroupRelationEntity> attrAttrgroupRelationEntities = this.relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", groupEntitie.getAttrGroupId()));
            if (!CollectionUtils.isEmpty(attrAttrgroupRelationEntities)) {
                List<Long> attrIds = attrAttrgroupRelationEntities.stream().map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());
                //3.根据spuId和attrId查询规格参数名及值
                List<ProductAttrValueEntity> attrValueEntityList = attrValueDao.selectList(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId).in("attr_id", attrIds));
                itemGroupVO.setBaseAttrValues(attrValueEntityList);
            }
            return itemGroupVO;
        }).collect(Collectors.toList());

    }

}