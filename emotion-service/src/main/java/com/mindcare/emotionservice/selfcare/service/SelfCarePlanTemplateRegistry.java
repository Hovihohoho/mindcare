package com.mindcare.emotionservice.selfcare.service;

import com.mindcare.emotionservice.selfcare.dto.SelfCarePlanTemplateResponse;
import com.mindcare.emotionservice.selfcare.dto.UpsertSelfCarePlanRequest;
import com.mindcare.emotionservice.selfcare.entity.SelfCareGoal;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SelfCarePlanTemplateRegistry {
    private static final String WHO_STRESS = "https://www.who.int/publications/i/item/9789240003927";
    private static final String WHO_STRESS_GUIDE = "https://cdn.who.int/media/docs/default-source/mental-health/july-2020.pdf?sfvrsn=bf75d66d_3";
    private static final String AASM_SLEEP = "https://aasm.org/wp-content/uploads/2021/08/Behavioral-and-Psychological-Treatments-for-Insomnia-Patient-Guide.pdf";
    private static final String VA_CBTI = "https://ptsd.va.gov/PTSD/appvid/mobile/cbticoach_app_public.asp";
    private static final String WHO_MACTIVE = "https://www.who.int/publications/i/item/9789240033474";
    private static final String WHO_MACTIVE_GUIDE = "https://iris.who.int/bitstream/10665/348214/1/9789240033474-eng.pdf";

    private final Map<String, SelfCarePlanTemplateResponse> templates;

    public SelfCarePlanTemplateRegistry() {
        Map<String, SelfCarePlanTemplateResponse> values = new LinkedHashMap<>();
        add(values, template("STRESS_WHO_V1", SelfCareGoal.REDUCE_STRESS, "Giảm căng thẳng",
                "Các bài thực hành tự hỗ trợ ngắn theo hướng dẫn của WHO.", WHO_STRESS, WHO_STRESS_GUIDE,
                List.of(activity("GROUNDING", "Thực hành Grounding", 5),
                        activity("NOTICE_AND_NAME", "Nhận biết và gọi tên suy nghĩ, cảm xúc", 5),
                        activity("UNHOOKING", "Thực hành gỡ khỏi suy nghĩ khó chịu", 3),
                        activity("ACT_ON_VALUES", "Thực hiện một hành động theo giá trị cá nhân", 3),
                        activity("SELF_KINDNESS", "Thực hành tử tế với bản thân", 3))));
        add(values, template("ANXIETY_WHO_V1", SelfCareGoal.MANAGE_ANXIETY, "Hỗ trợ quản lý lo âu",
                "Các bài thực hành tự hỗ trợ ngắn theo hướng dẫn của WHO.", WHO_STRESS, WHO_STRESS_GUIDE,
                List.of(activity("GROUNDING", "Thực hành Grounding", 5),
                        activity("NOTICE_AND_NAME", "Nhận biết và gọi tên điều đang xuất hiện", 5),
                        activity("UNHOOKING", "Thực hành gỡ khỏi suy nghĩ lo âu", 3),
                        activity("MAKING_ROOM", "Tạo không gian cho cảm xúc khó chịu", 3),
                        activity("ACT_ON_VALUES", "Thực hiện một hành động theo giá trị cá nhân", 3))));
        add(values, template("SLEEP_WELLNESS_V1", SelfCareGoal.IMPROVE_SLEEP, "Hỗ trợ giấc ngủ",
                "Thói quen theo dõi và hỗ trợ giấc ngủ; không phải chương trình điều trị CBT-I.", AASM_SLEEP, VA_CBTI,
                List.of(activity("SLEEP_DIARY", "Ghi giờ ngủ, giờ thức và cảm nhận sau khi ngủ", 7),
                        activity("CONSISTENT_WAKE_TIME", "Thức dậy theo giờ đã chọn", 7),
                        activity("PRE_SLEEP_RELAXATION", "Thực hành thư giãn trước khi ngủ", 5),
                        activity("QUIET_SLEEP_ENVIRONMENT", "Chuẩn bị không gian ngủ yên tĩnh, tối và thoải mái", 5),
                        activity("WEEKLY_SLEEP_REVIEW", "Xem lại nhật ký giấc ngủ cuối tuần", 1))));
        add(values, template("LOW_ACTIVITY_MACTIVE_V1", SelfCareGoal.BUILD_BALANCE, "Tăng vận động bằng đi bộ",
                "Chương trình khởi đầu bằng đi bộ ngắn và tăng dần theo khả năng.", WHO_MACTIVE, WHO_MACTIVE_GUIDE,
                List.of(activity("SET_WALKING_GOAL", "Đặt mục tiêu đi bộ phù hợp trong tuần", 1),
                        activity("WALK_10_MINUTES", "Đi bộ ít nhất 10 phút", 7),
                        activity("TRACK_DAILY_STEPS", "Theo dõi số bước trong ngày", 7),
                        activity("GRADUAL_WALK_INCREASE", "Tăng dần thời gian đi bộ nếu cảm thấy phù hợp", 3),
                        activity("REVIEW_WALKING_PROGRESS", "Xem lại tiến độ đi bộ cuối tuần", 1))));
        templates = Map.copyOf(values);
    }

    public List<SelfCarePlanTemplateResponse> all() { return List.copyOf(templates.values()); }

    public SelfCarePlanTemplateResponse require(String code) {
        SelfCarePlanTemplateResponse template = templates.get(code);
        if (template == null) throw new InvalidRequestException("UNKNOWN_PLAN_TEMPLATE", "Plan template is not supported");
        return template;
    }

    public SelfCarePlanTemplateResponse match(UpsertSelfCarePlanRequest request) {
        return templates.values().stream().filter(template -> same(template, request)).findFirst()
                .orElseThrow(() -> new InvalidRequestException("PLAN_TEMPLATE_REQUIRED", "Kế hoạch phải khớp một form cố định đã được duyệt"));
    }

    private boolean same(SelfCarePlanTemplateResponse template, UpsertSelfCarePlanRequest request) {
        if (template.goal() != request.goal() || template.activities().size() != request.activities().size()) return false;
        for (int index = 0; index < template.activities().size(); index++) {
            var expected = template.activities().get(index);
            var actual = request.activities().get(index);
            if (!expected.activityCode().equals(actual.activityCode()) || !expected.title().equals(actual.title().trim())
                    || expected.targetPerWeek() != actual.targetPerWeek()) return false;
        }
        return true;
    }

    private void add(Map<String, SelfCarePlanTemplateResponse> values, SelfCarePlanTemplateResponse template) {
        values.put(template.templateCode(), template);
    }

    private SelfCarePlanTemplateResponse template(String code, SelfCareGoal goal, String title, String description,
                                                   String source, String implementation, List<SelfCarePlanTemplateResponse.ActivityTemplateResponse> activities) {
        return new SelfCarePlanTemplateResponse(code, "1.0", goal, title, description, activities,
                source.contains("who.int") ? "World Health Organization" : "American Academy of Sleep Medicine / U.S. Department of Veterans Affairs",
                source, implementation, "Công cụ tự chăm sóc và xây dựng thói quen; không thay thế chẩn đoán hoặc điều trị.");
    }

    private SelfCarePlanTemplateResponse.ActivityTemplateResponse activity(String code, String title, int target) {
        return new SelfCarePlanTemplateResponse.ActivityTemplateResponse(code, title, target);
    }
}
