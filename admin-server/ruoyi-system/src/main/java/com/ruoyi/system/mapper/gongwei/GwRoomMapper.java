package com.ruoyi.system.mapper.gongwei;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.gongwei.GwRoom;

@Mapper
public interface GwRoomMapper {
    List<GwRoom> selectRoomList(GwRoom room);
    GwRoom selectRoomById(Long roomId);
    GwRoom selectRoomForUpdate(Long roomId);
    int insertRoom(GwRoom room);
    int updateRoom(GwRoom room);
    int deleteRoomById(Long roomId);
    int countRoomReferences(Long roomId);
    int countWorkstations(Long roomId);
    int countActiveAssignments(Long roomId);
    int countPendingStudioApplications(Long roomId);
    int bindMentor(@Param("roomId") Long roomId, @Param("mentorId") Long mentorId);
    int releaseMentor(@Param("roomId") Long roomId, @Param("mentorId") Long mentorId);
}
