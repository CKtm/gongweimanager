package com.ruoyi.system.service.gongwei;

import java.util.List;
import com.ruoyi.system.domain.gongwei.GwPersonImportRow;

public interface IGwPersonImportService {
    String importPeople(List<GwPersonImportRow> rows, String operator);
}
