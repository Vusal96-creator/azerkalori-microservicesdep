package az.azerkalori.nutrition.grpc;

import az.azerkalori.nutrition.proto.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class NutritionGrpcService
        extends NutritionCalculationServiceGrpc.NutritionCalculationServiceImplBase {

    @Override
    public void calculateGoal(CalculateGoalRequest req,
                              StreamObserver<CalculateGoalResponse> observer) {

        double bmr = 10 * req.getWeightKg()
                   + 6.25 * req.getHeightCm()
                   - 5 * req.getAge()
                   + (req.getSex() == Sex.MALE ? 5 : -161);

        double multiplier = switch (req.getActivityLevel()) {
            case SEDENTARY -> 1.2;
            case LIGHT -> 1.375;
            case MODERATE -> 1.55;
            case ACTIVE -> 1.725;
            default -> 1.9;
        };
        double tdee = bmr * multiplier;

        double daily = switch (req.getGoal()) {
            case LOSE -> tdee * 0.85;
            case GAIN -> tdee * 1.15;
            default -> tdee;
        };

        double proteinG = req.getWeightKg() * 1.8;
        double fatG = daily * 0.25 / 9;
        double carbsG = (daily - proteinG * 4 - fatG * 9) / 4;

        observer.onNext(CalculateGoalResponse.newBuilder()
                .setBmr(round(bmr))
                .setTdee(round(tdee))
                .setDailyCalories(round(daily))
                .setProteinG(round(proteinG))
                .setFatG(round(fatG))
                .setCarbsG(round(Math.max(carbsG, 0)))
                .build());
        observer.onCompleted();
    }

    private double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
