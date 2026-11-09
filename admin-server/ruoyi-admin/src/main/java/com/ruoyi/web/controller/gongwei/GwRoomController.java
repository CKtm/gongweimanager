package com.ruoyi.web.controller.gongwei;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.gongwei.GwRoom;
import com.ruoyi.system.service.gongwei.IGwRoomService;

@RestController
@RequestMapping("/gw/room")
public class GwRoomController extends BaseController {
    @Autowired
    private IGwRoomService roomService;

    @PreAuthorize("@ss.hasPermi('gw:room:list')")
    @GetMapping("/list")
    public AjaxResult list(GwRoom room) {
        return success(roomService.selectRoomList(room));
    }

    @PreAuthorize("@ss.hasPermi('gw:room:query')")
    @GetMapping("/{roomId}")
    public AjaxResult getInfo(@PathVariable Long roomId) {
        return success(roomService.selectRoomById(roomId));
    }

    @PreAuthorize("@ss.hasPermi('gw:room:add')")
    @Log(title = "房间管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody GwRoom room) {
        room.setCreateBy(getUsername());
        return toAjax(roomService.insertRoom(room));
    }

    @PreAuthorize("@ss.hasPermi('gw:room:edit')")
    @Log(title = "房间管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody GwRoom room) {
        room.setUpdateBy(getUsername());
        return toAjax(roomService.updateRoom(room));
    }

    @PreAuthorize("@ss.hasPermi('gw:room:remove')")
    @Log(title = "房间管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roomId}")
    public AjaxResult remove(@PathVariable Long roomId) {
        return toAjax(roomService.deleteRoomById(roomId));
    }
}
