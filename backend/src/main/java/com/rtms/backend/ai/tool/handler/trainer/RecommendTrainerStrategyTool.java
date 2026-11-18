package com.rtms.backend.ai.tool.handler.trainer;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RecommendTrainerStrategyTool implements AiToolHandler {

    private final RecommendOptimalRaceDistanceAndTrackTool raceTool;
    private final RecommendNextWorkoutAdjustmentTool workoutTool;
    private final AiToolSupport support;

    @Autowired
    public RecommendTrainerStrategyTool(RecommendOptimalRaceDistanceAndTrackTool raceTool,
                                         RecommendNextWorkoutAdjustmentTool workoutTool,
                                         AiToolSupport support) {
        this.raceTool = raceTool;
        this.workoutTool = workoutTool;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "recommendTrainerStrategy";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "A horse ID or name must be provided");
        }
        Object raceObj = raceTool.execute(arguments, currentUser);
        Object workoutObj = workoutTool.execute(arguments, currentUser);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenNgua", horse.getName());
        result.put("maDangKy", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("chienLuocCuLyVaSanDua", raceObj);
        result.put("dieuChinhBaiTapKeTiep", workoutObj);
        return result;
    }
}
