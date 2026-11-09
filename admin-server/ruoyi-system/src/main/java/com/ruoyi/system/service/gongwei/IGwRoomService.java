package com.ruoyi.system.service.gongwei;

import java.util.List;
import com.ruoyi.system.domain.gongwei.GwRoom;

public interface IGwRoomService {
    List<GwRoom> selectRoomList(GwRoom room);
    GwRoom selectRoomById(Long roomId);
    int insertRoom(GwRoom room);
    int updateRoom(GwRoom room);
    int deleteRoomById(Long roomId);
}
