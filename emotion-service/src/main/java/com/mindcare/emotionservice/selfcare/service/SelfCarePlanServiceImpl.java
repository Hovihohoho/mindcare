package com.mindcare.emotionservice.selfcare.service;

import com.mindcare.emotionservice.selfcare.dto.*;
import com.mindcare.emotionservice.selfcare.entity.*;
import com.mindcare.emotionservice.selfcare.repository.*;
import com.mindcare.emotionservice.healthmetric.service.HealthBenchmarkService;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import com.mindcare.emotionservice.shared.exception.ResourceNotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SelfCarePlanServiceImpl implements SelfCarePlanService {
    private final SelfCarePlanRepository planRepository;
    private final SelfCareActivityRepository activityRepository;
    private final SelfCareCompletionRepository completionRepository;
    private final SelfCarePlanTemplateRegistry templateRegistry;
    private final HealthBenchmarkService healthBenchmarkService;

    @Override
    @Transactional(readOnly = true)
    public SelfCarePlanResponse get(UUID userId, LocalDate today) {
        return response(requirePlan(userId), userId, today);
    }

    @Override
    @Transactional
    public SelfCarePlanResponse upsert(UUID userId, UpsertSelfCarePlanRequest request, LocalDate today) {
        ensureUniqueCodes(request.activities());
        return saveTemplate(userId, templateRegistry.match(request), today);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SelfCarePlanTemplateResponse> templates() {
        return templateRegistry.all();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SelfCarePlanRecommendationResponse> recommendations(UUID userId) {
        return healthBenchmarkService.evaluate(userId).stream()
                .filter(item -> item.alertTriggered() && item.recommendedPlanTemplateCode() != null)
                .map(item -> new SelfCarePlanRecommendationResponse(item.recommendedPlanTemplateCode(), item.reasonCode(),
                        item.message(), item.observedValue(), item.unit(), item.policyKey(), item.policyVersion(), item.sourceUrl()))
                .toList();
    }

    @Override
    @Transactional
    public SelfCarePlanResponse applyTemplate(UUID userId, String templateCode, LocalDate today) {
        return saveTemplate(userId, templateRegistry.require(templateCode), today);
    }

    private SelfCarePlanResponse saveTemplate(UUID userId, SelfCarePlanTemplateResponse template, LocalDate today) {
        SelfCarePlanEntity plan = planRepository.findByUserId(userId)
                .orElseGet(() -> new SelfCarePlanEntity(userId, template.goal()));
        List<SelfCareActivityEntity> activities = new ArrayList<>();
        for (int index = 0; index < template.activities().size(); index++) {
            var item = template.activities().get(index);
            activities.add(new SelfCareActivityEntity(
                    item.activityCode(), item.title().trim(), item.targetPerWeek(), index));
        }
        plan.replace(template.goal(), activities);
        plan.applyTemplate(template.templateCode(), template.templateVersion(), template.sourceUrl());
        return response(planRepository.saveAndFlush(plan), userId, today);
    }

    @Override
    @Transactional
    public SelfCarePlanResponse complete(UUID userId, UUID activityId, LocalDate date, LocalDate today) {
        validateCompletionDate(date, today);
        SelfCareActivityEntity activity = requireActivity(userId, activityId);
        completionRepository.findByActivityIdAndCompletedOn(activityId, date)
                .orElseGet(() -> completionRepository.save(new SelfCareCompletionEntity(activity, date)));
        return response(requirePlan(userId), userId, today);
    }

    @Override
    @Transactional
    public SelfCarePlanResponse undo(UUID userId, UUID activityId, LocalDate date, LocalDate today) {
        validateCompletionDate(date, today);
        requireActivity(userId, activityId);
        completionRepository.findByActivityIdAndCompletedOn(activityId, date)
                .ifPresent(completionRepository::delete);
        completionRepository.flush();
        return response(requirePlan(userId), userId, today);
    }

    private SelfCarePlanEntity requirePlan(UUID userId) {
        return planRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Self-care plan"));
    }

    private SelfCareActivityEntity requireActivity(UUID userId, UUID activityId) {
        return activityRepository.findByIdAndPlanUserId(activityId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Self-care activity"));
    }

    private SelfCarePlanResponse response(SelfCarePlanEntity plan, UUID userId, LocalDate today) {
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<SelfCareCompletionEntity> completions = completionRepository
                .findByActivityPlanUserIdAndCompletedOnBetween(userId, weekStart, today);
        Map<UUID, Long> counts = completions.stream().collect(Collectors.groupingBy(
                item -> item.getActivity().getId(), Collectors.counting()));
        Set<UUID> completedToday = completions.stream()
                .filter(item -> item.getCompletedOn().equals(today))
                .map(item -> item.getActivity().getId()).collect(Collectors.toSet());
        List<SelfCarePlanResponse.ActivityResponse> activities = plan.getActivities().stream().map(item ->
                new SelfCarePlanResponse.ActivityResponse(item.getId(), item.getActivityCode(), item.getTitle(),
                        item.getTargetPerWeek(), counts.getOrDefault(item.getId(), 0L).intValue(),
                        completedToday.contains(item.getId()))).toList();
        return new SelfCarePlanResponse(plan.getId(), plan.getGoal(), plan.getTemplateCode(), plan.getTemplateVersion(), plan.getSourceUrl(), weekStart,
                activities.stream().mapToInt(SelfCarePlanResponse.ActivityResponse::completedThisWeek).sum(),
                activities.stream().mapToInt(SelfCarePlanResponse.ActivityResponse::targetPerWeek).sum(),
                activities, plan.getUpdatedAt());
    }

    private void ensureUniqueCodes(List<UpsertSelfCarePlanRequest.ActivityRequest> activities) {
        Set<String> seen = new HashSet<>();
        if (activities.stream().map(UpsertSelfCarePlanRequest.ActivityRequest::activityCode).anyMatch(code -> !seen.add(code))) {
            throw new InvalidRequestException("DUPLICATE_ACTIVITY_CODE", "Mỗi hoạt động chỉ được xuất hiện một lần");
        }
    }

    private void validateCompletionDate(LocalDate date, LocalDate today) {
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (date.isAfter(today) || date.isBefore(weekStart)) {
            throw new InvalidRequestException("COMPLETION_DATE_OUT_OF_RANGE", "Chỉ có thể cập nhật hoạt động trong tuần hiện tại");
        }
    }
}
