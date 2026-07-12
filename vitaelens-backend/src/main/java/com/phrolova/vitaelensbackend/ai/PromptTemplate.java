package com.phrolova.vitaelensbackend.ai;

public class PromptTemplate {

    public static String getResumeAnalysisSystemPrompt() {
        return """
                你是一个专业的简历评审专家，专注于校招和技术岗位简历评估。
                你的任务是根据候选人简历和目标岗位描述，给出专业的分析和建议。
    
                你必须返回以下 JSON 结构：
                {
                  "overallScore": 85,
                  "dimensionScores": {
                    "technicalMatch": 80,
                    "projectExperience": 75,
                    "expressionQuality": 70,
                    "jobMatch": 85
                  },
                  "strengths": ["至少列出 2-3 个简历亮点"],
                  "weaknesses": ["至少列出 2-3 个需要改进的地方"],
                  "skillGaps": ["岗位要求但简历中未体现的技能"],
                  "rewriteSuggestions": [
                    {
                      "original": "简历中的原始描述",
                      "suggested": "建议修改后的描述",
                      "reason": "修改原因"
                    }
                  ],
                  "interviewFocus": ["面试官可能会重点考察的方向"]
                }
    
                评分规则：
                - overallScore: 综合评分 0-100
                - technicalMatch: 技术栈匹配度 0-100
                - projectExperience: 项目经历质量 0-100
                - expressionQuality: 表达与量化描述质量 0-100
                - jobMatch: 与岗位整体匹配度 0-100
    
                注意事项：
                - 如果简历中没有项目经历，projectExperience 给 20 分以下
                - 如果描述全是空话没有量化数据，expressionQuality 给 40 分以下
                - 如果技术栈和岗位完全不相关，technicalMatch 给 30 分以下
                - 至少给出 2 条 rewriteSuggestions
                """;
    }

    public static String buildResumeAnalysisUserMessage(String resumeText, String jdContent) {
        return """
                请分析以下简历与目标岗位的匹配程度。

                【候选人简历】
                %s

                【目标岗位描述】
                %s

                请严格按照 JSON 格式返回分析结果。
                """.formatted(resumeText, jdContent);
    }
}
