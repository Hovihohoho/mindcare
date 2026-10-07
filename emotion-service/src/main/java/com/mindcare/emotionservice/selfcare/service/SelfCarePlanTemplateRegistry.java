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
    private static final String WHO_STRESS_TITLE = "Doing What Matters in Times of Stress: An Illustrated Guide";
    private static final String AASM_SLEEP_TITLE = "A patient's guide to understanding Behavioral and Psychological Treatments for Chronic Insomnia Disorder in Adults";
    private static final String VA_CBTI_TITLE = "CBT-i Coach — U.S. Department of Veterans Affairs";
    private static final String WHO_MACTIVE_TITLE = "Be He@lthy, be mobile: a handbook on how to implement mobile health for physical activity";

    private final Map<String, SelfCarePlanTemplateResponse> templates;

    public SelfCarePlanTemplateRegistry() {
        Map<String, SelfCarePlanTemplateResponse> values = new LinkedHashMap<>();
        add(values, template("STRESS_WHO_V1", SelfCareGoal.REDUCE_STRESS, "Giảm căng thẳng",
                "Các bài thực hành tự hỗ trợ ngắn theo hướng dẫn của WHO.", WHO_STRESS, WHO_STRESS_GUIDE,
                List.of(whoActivity("GROUNDING", "Thực hành Grounding", 5, "Grounding",
                                "WHO xếp grounding là một kỹ năng thực hành để ứng phó với căng thẳng."),
                        whoActivity("NOTICE_AND_NAME", "Nhận biết và gọi tên suy nghĩ, cảm xúc", 5, "Unhooking from difficult thoughts",
                                "Kỹ năng unhooking trong hướng dẫn giúp nhận ra suy nghĩ khó chịu và gọi chúng là suy nghĩ."),
                        whoActivity("UNHOOKING", "Thực hành gỡ khỏi suy nghĩ khó chịu", 3, "Unhooking from difficult thoughts",
                                "Hoạt động dựa trên kỹ năng tạo khoảng cách với suy nghĩ khó chịu trong hướng dẫn."),
                        whoActivity("ACT_ON_VALUES", "Thực hiện một hành động theo giá trị cá nhân", 3, "Acting on your values",
                                "Hướng dẫn có kỹ năng thực hiện hành động theo giá trị cá nhân."),
                        whoActivity("SELF_KINDNESS", "Thực hành tử tế với bản thân", 3, "Being kind to yourself",
                                "Hướng dẫn có kỹ năng đối xử tử tế với bản thân khi gặp khó khăn."))));
        add(values, template("ANXIETY_WHO_V1", SelfCareGoal.MANAGE_ANXIETY, "Hỗ trợ quản lý lo âu",
                "Các bài thực hành tự hỗ trợ ngắn theo hướng dẫn của WHO.", WHO_STRESS, WHO_STRESS_GUIDE,
                List.of(whoActivity("GROUNDING", "Thực hành Grounding", 5, "Grounding",
                                "WHO xếp grounding là một kỹ năng thực hành để ứng phó với căng thẳng."),
                        whoActivity("NOTICE_AND_NAME", "Nhận biết và gọi tên điều đang xuất hiện", 5, "Unhooking from difficult thoughts",
                                "Kỹ năng unhooking trong hướng dẫn giúp nhận ra suy nghĩ khó chịu và gọi chúng là suy nghĩ."),
                        whoActivity("UNHOOKING", "Thực hành gỡ khỏi suy nghĩ lo âu", 3, "Unhooking from difficult thoughts",
                                "Hoạt động dựa trên kỹ năng tạo khoảng cách với suy nghĩ khó chịu trong hướng dẫn."),
                        whoActivity("MAKING_ROOM", "Tạo không gian cho cảm xúc khó chịu", 3, "Making room for difficult thoughts and feelings",
                                "Hướng dẫn có kỹ năng tạo không gian cho suy nghĩ và cảm xúc khó chịu."),
                        whoActivity("ACT_ON_VALUES", "Thực hiện một hành động theo giá trị cá nhân", 3, "Acting on your values",
                                "Hướng dẫn có kỹ năng thực hiện hành động theo giá trị cá nhân."))));
        add(values, template("SLEEP_WELLNESS_V1", SelfCareGoal.IMPROVE_SLEEP, "Hỗ trợ giấc ngủ",
                "Thói quen theo dõi và hỗ trợ giấc ngủ; không phải chương trình điều trị CBT-I.", AASM_SLEEP, VA_CBTI,
                List.of(sleepActivity("SLEEP_DIARY", "Ghi giờ ngủ, giờ thức và cảm nhận sau khi ngủ", 7, VA_CBTI_TITLE, VA_CBTI,
                                "Sleep diary", "VA mô tả nhật ký để ghi lại giờ ngủ và giờ thức."),
                        sleepActivity("CONSISTENT_WAKE_TIME", "Thức dậy theo giờ đã chọn", 7, VA_CBTI_TITLE, VA_CBTI,
                                "Wake-time reminders and sleep routines", "CBT-i Coach có nhắc giờ thức; hoạt động này dùng giờ thức đã chọn làm gợi nhắc thói quen."),
                        sleepActivity("PRE_SLEEP_RELAXATION", "Thực hành thư giãn trước khi ngủ", 5, AASM_SLEEP_TITLE, AASM_SLEEP,
                                "Relaxation Therapy", "AASM mô tả bài tập thư giãn trước khi ngủ, gồm thở và thả lỏng cơ."),
                        sleepActivity("QUIET_SLEEP_ENVIRONMENT", "Chuẩn bị không gian ngủ yên tĩnh, tối và thoải mái", 5, VA_CBTI_TITLE, VA_CBTI,
                                "Positive sleep routines and sleep environment", "VA giới thiệu cách xây dựng thói quen ngủ tích cực và cải thiện môi trường ngủ."),
                        sleepActivity("WEEKLY_SLEEP_REVIEW", "Xem lại nhật ký giấc ngủ cuối tuần", 1, VA_CBTI_TITLE, VA_CBTI,
                                "Sleep diary and personalized feedback", "VA mô tả việc ghi nhật ký và phản hồi về giấc ngủ; xem lại hằng tuần là thói quen tự theo dõi."))));
        add(values, template("LOW_ACTIVITY_MACTIVE_V1", SelfCareGoal.BUILD_BALANCE, "Tăng vận động bằng đi bộ",
                "Chương trình khởi đầu bằng đi bộ ngắn và tăng dần theo khả năng.", WHO_MACTIVE, WHO_MACTIVE_GUIDE,
                List.of(mActiveActivity("SET_WALKING_GOAL", "Đặt mục tiêu đi bộ phù hợp trong tuần", 1, "mActive walking programme and incremental goals",
                                "Sổ tay mô tả chương trình đi bộ với các mục tiêu tăng dần."),
                        mActiveActivity("WALK_10_MINUTES", "Đi bộ ít nhất 10 phút", 7, "Walking as an accessible starting activity",
                                "Sổ tay mô tả đi bộ là cách bắt đầu cho người ít vận động; mốc 10 phút là mục tiêu thói quen nhỏ của template này."),
                        mActiveActivity("TRACK_DAILY_STEPS", "Theo dõi số bước trong ngày", 7, "Self-monitoring and physical-activity tracking",
                                "Sổ tay đề cập hỗ trợ theo dõi hoạt động thể chất bằng công nghệ di động."),
                        mActiveActivity("GRADUAL_WALK_INCREASE", "Tăng dần thời gian đi bộ nếu cảm thấy phù hợp", 3, "Walking and gradual increase",
                                "Sổ tay mô tả việc bắt đầu bằng đi bộ và tăng hoạt động dần dần."),
                        mActiveActivity("REVIEW_WALKING_PROGRESS", "Xem lại tiến độ đi bộ cuối tuần", 1, "Incremental goals and programme review",
                                "Sổ tay mô tả mục tiêu theo từng tuần; hoạt động này là thói quen tự xem lại tiến độ."))));
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

    private SelfCarePlanTemplateResponse.ActivityTemplateResponse whoActivity(String code, String title, int target,
                                                                               String section, String note) {
        return evidenceActivity(code, title, target, WHO_STRESS_TITLE, WHO_STRESS, section, note);
    }

    private SelfCarePlanTemplateResponse.ActivityTemplateResponse sleepActivity(String code, String title, int target,
                                                                                 String sourceTitle, String sourceUrl,
                                                                                 String section, String note) {
        return evidenceActivity(code, title, target, sourceTitle, sourceUrl, section, note);
    }

    private SelfCarePlanTemplateResponse.ActivityTemplateResponse mActiveActivity(String code, String title, int target,
                                                                                    String section, String note) {
        return evidenceActivity(code, title, target, WHO_MACTIVE_TITLE, WHO_MACTIVE, section, note);
    }

    private SelfCarePlanTemplateResponse.ActivityTemplateResponse evidenceActivity(String code, String title, int target,
                                                                                     String sourceTitle, String sourceUrl,
                                                                                     String section, String note) {
        return new SelfCarePlanTemplateResponse.ActivityTemplateResponse(code, title, target, sourceTitle, sourceUrl, section, note);
    }
}
