package az.azerkalori.nutrition.web;

import az.azerkalori.nutrition.proto.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    @GrpcClient("nutrition")
    private NutritionCalculationServiceGrpc.NutritionCalculationServiceBlockingStub stub;

    @PostMapping("/calculate")
    public Map<String, Double> calculate(@RequestBody GoalRequest req) {
        CalculateGoalResponse resp = stub.calculateGoal(CalculateGoalRequest.newBuilder()
                .setAge(req.age())
                .setWeightKg(req.weightKg())
                .setHeightCm(req.heightCm())
                .setSex(Sex.valueOf(req.sex()))
                .setActivityLevel(ActivityLevel.valueOf(req.activityLevel()))
                .setGoal(GoalType.valueOf(req.goal()))
                .build());

        return Map.of(
                "bmr", resp.getBmr(),
                "tdee", resp.getTdee(),
                "dailyCalories", resp.getDailyCalories(),
                "proteinG", resp.getProteinG(),
                "fatG", resp.getFatG(),
                "carbsG", resp.getCarbsG());
    }

    public record GoalRequest(int age, double weightKg, double heightCm,
                              String sex, String activityLevel, String goal) {}
}
