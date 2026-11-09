package com.ruoyi.system.service.gongwei.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.gongwei.GwPersonImportRow;
import com.ruoyi.system.domain.gongwei.GwUserProfile;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.mapper.gongwei.GwUserProfileMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.gongwei.IGwPersonImportService;

@Service
public class GwPersonImportServiceImpl implements IGwPersonImportService {
    @Autowired private SysUserMapper userMapper;
    @Autowired private SysRoleMapper roleMapper;
    @Autowired private GwUserProfileMapper profileMapper;
    @Autowired private ISysUserService userService;
    @Autowired private ISysConfigService configService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importPeople(List<GwPersonImportRow> rows, String operator) {
        if (rows == null || rows.isEmpty()) throw new ServiceException("导入数据不能为空");
        String initialPassword = configService.selectConfigByKey("sys.user.initPassword");
        if (initialPassword == null || initialPassword.isBlank()) throw new ServiceException("系统尚未配置安全的初始密码策略");
        Map<String, Long> roleIds = loadRoleIds();
        Set<String> accounts = new HashSet<>();
        Set<String> identities = new HashSet<>();
        Map<String, Long> importedIdentityUsers = new HashMap<>();
        int rowNo = 1;
        for (GwPersonImportRow row : rows) {
            rowNo++;
            normalizeAndValidate(row, rowNo, roleIds);
            String accountKey = row.getUserName().toLowerCase(Locale.ROOT);
            String identityKey = row.getIdentityNo().toLowerCase(Locale.ROOT);
            if (!accounts.add(accountKey)) throw new ServiceException("第" + rowNo + "行登录账号在文件内重复");
            if (!identities.add(identityKey)) throw new ServiceException("第" + rowNo + "行学号/工号在文件内重复");
            if (userMapper.selectUserByUserName(row.getUserName()) != null) throw new ServiceException("第" + rowNo + "行登录账号已存在");
            if (profileMapper.selectUserIdByIdentityNo(row.getIdentityNo()) != null) throw new ServiceException("第" + rowNo + "行学号/工号已存在");
        }
        for (GwPersonImportRow row : rows) {
            SysUser user = new SysUser();
            user.setUserName(row.getUserName());
            user.setNickName(row.getNickName());
            user.setDeptId(row.getDeptId());
            user.setPhonenumber(blankToNull(row.getPhonenumber()));
            user.setEmail(blankToNull(row.getEmail()));
            user.setSex("2");
            user.setStatus("0");
            user.setPassword(SecurityUtils.encryptPassword(initialPassword));
            user.setCreateBy(operator);
            user.setRoleIds(new Long[] { roleIds.get(row.getUserType()) });
            userService.insertUser(user);
            GwUserProfile profile = new GwUserProfile();
            profile.setUserId(user.getUserId());
            profile.setIdentityNo(row.getIdentityNo());
            profile.setUserType(row.getUserType());
            profileMapper.upsert(profile);
            importedIdentityUsers.put(row.getIdentityNo().toLowerCase(Locale.ROOT), user.getUserId());
        }
        for (GwPersonImportRow row : rows) {
            if (!"STUDENT".equals(row.getUserType()) || row.getMentorIdentityNo() == null || row.getMentorIdentityNo().isBlank()) continue;
            Long mentorId = importedIdentityUsers.get(row.getMentorIdentityNo().toLowerCase(Locale.ROOT));
            if (mentorId == null) mentorId = profileMapper.selectUserIdByIdentityNo(row.getMentorIdentityNo());
            if (mentorId == null || profileMapper.countActiveUserByRole(mentorId, "mentor") == 0) throw new ServiceException("学生 " + row.getIdentityNo() + " 填写的导师工号不存在或未分配导师角色");
            Long studentId = importedIdentityUsers.get(row.getIdentityNo().toLowerCase(Locale.ROOT));
            profileMapper.bindMentor(studentId, mentorId);
        }
        return "成功导入 " + rows.size() + " 名人员；账号已按角色初始化，首次登录必须修改初始密码";
    }

    private Map<String, Long> loadRoleIds() {
        Map<String, Long> ids = new HashMap<>();
        ids.put("STUDENT", roleId("common"));
        ids.put("MENTOR", roleId("mentor"));
        ids.put("INSPECTOR", roleId("inspector"));
        return ids;
    }

    private Long roleId(String roleKey) {
        SysRole role = roleMapper.checkRoleKeyUnique(roleKey);
        if (role == null || !"0".equals(role.getStatus())) throw new ServiceException("缺少可用角色：" + roleKey);
        return role.getRoleId();
    }

    private void normalizeAndValidate(GwPersonImportRow row, int rowNo, Map<String, Long> roleIds) {
        if (row == null) throw new ServiceException("第" + rowNo + "行数据为空");
        row.setUserName(trim(row.getUserName()));
        row.setNickName(trim(row.getNickName()));
        row.setIdentityNo(trim(row.getIdentityNo()));
        row.setUserType(trim(row.getUserType()).toUpperCase(Locale.ROOT));
        row.setMentorIdentityNo(trim(row.getMentorIdentityNo()));
        if (row.getUserName().isBlank() || row.getNickName().isBlank() || row.getIdentityNo().isBlank() || row.getUserType().isBlank()) throw new ServiceException("第" + rowNo + "行账号、姓名、学号/工号和身份类型不能为空");
        if (row.getUserName().length() > 30 || row.getNickName().length() > 30 || row.getIdentityNo().length() > 64) throw new ServiceException("第" + rowNo + "行字段长度超出限制");
        if (!roleIds.containsKey(row.getUserType())) throw new ServiceException("第" + rowNo + "行身份类型只能是 STUDENT、MENTOR 或 INSPECTOR");
        if (!"STUDENT".equals(row.getUserType()) && !row.getMentorIdentityNo().isBlank()) throw new ServiceException("第" + rowNo + "行只有学生可以填写导师工号");
    }

    private String trim(String value) { return value == null ? "" : value.trim(); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
