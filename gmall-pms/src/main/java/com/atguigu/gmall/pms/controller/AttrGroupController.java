package com.atguigu.gmall.pms.controller;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import com.atguigu.gmall.pms.vo.GroupVo;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;


/**
 * 属性分组
 *
 * @author suxiaohu
 * @email lxf@atguigu.com
 * @date 2020-01-02 17:13:27
 */
@Api(tags = "属性分组 管理")
@RestController
@RequestMapping("pms/attrgroup")
public class AttrGroupController {

    @Autowired
    private AttrGroupService attrGroupService;


    @GetMapping("withattrsvalues")
    public Resp<List<ItemGroupVO>>  queryItemGroupVOsByCidAndSpuId(
                    @RequestParam("cid") Long cid
            ,@RequestParam("spuId") Long spuId
    ){
        List<ItemGroupVO>  itemGroupVOS = attrGroupService.queryItemGroupVOsByCidAndSpuId(cid,spuId);
        return Resp.ok(itemGroupVOS);
    }


    @GetMapping("withattrs/cat/{catId}")
    public Resp<List<GroupVo>> queryGroupVoByCid(@PathVariable("catId") long cid) {
        List<GroupVo> groupVos = attrGroupService.queryGroupVoByCid(cid);
        return Resp.ok(groupVos);
    }

    /**
     * 请求路径：http://127.0.0.1:8888/pms/attr?type=1&cid=225
     */
    @GetMapping("{catId}")
    public Resp<PageVo> list(QueryCondition queryCondition, @PathVariable("catId") Long catId) {
        PageVo page = attrGroupService.querGroupByCidPage(queryCondition, catId);
        return Resp.ok(page);
    }

    /**
     * http://127.0.0.1:8888/pms/attrgroup/withattr/1
     * 查询组下的规格参数(属性组关联)
     *
     * @param gid
     * @return
     */
    @GetMapping("/withattr/{gid}")
    public Resp<GroupVo> queryGroupVoByGid(@PathVariable("gid") Long gid) {
        GroupVo groupVo = attrGroupService.queryGroupVoByGid(gid);
        return Resp.ok(groupVo);
    }


    /**
     * 列表
     */
    @ApiOperation("分页查询(排序)")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('pms:attrgroup:list')")
    public Resp<PageVo> list(QueryCondition queryCondition) {
        PageVo page = attrGroupService.queryPage(queryCondition);

        return Resp.ok(page);
    }


    /**
     * 信息
     */
    @ApiOperation("详情查询")
    @GetMapping("/info/{attrGroupId}")
    @PreAuthorize("hasAuthority('pms:attrgroup:info')")
    public Resp<AttrGroupEntity> info(@PathVariable("attrGroupId") Long attrGroupId) {
        AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);

        return Resp.ok(attrGroup);
    }

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('pms:attrgroup:save')")
    public Resp<Object> save(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.save(attrGroup);

        return Resp.ok(null);
    }

    /**
     * 修改
     */
    @ApiOperation("修改")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('pms:attrgroup:update')")
    public Resp<Object> update(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.updateById(attrGroup);

        return Resp.ok(null);
    }

    /**
     * 删除
     */
    @ApiOperation("删除")
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('pms:attrgroup:delete')")
    public Resp<Object> delete(@RequestBody Long[] attrGroupIds) {
        attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return Resp.ok(null);
    }

}
