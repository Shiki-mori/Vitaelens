package com.phrolova.vitaelensbackend.ai;

import java.util.Map;

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

    public static String getInterviewQuestionPrompt(){
        return """
                你是一个技术面试官，请根据候选人的简历和目标岗位，生成 5-8 个面试问题。
                问题应该覆盖：项目经历追问、技术基础、场景题。
                项目追问要具体到简历中提到的技术和实现细节，不要问泛泛的问题。
                
                返回 JSON 数组：
                [
                  {
                    "category": "project" | "fundamental" | "scenario",
                    "question": "问题内容",
                    "focus": "考察要点"
                  }
                ]
                
                只返回 JSON 数组，不要包含其他文本。
                """;
    }

    public static String buildInterviewQuestionMessage(
            String resumeText, String jdContent, Map<String, Object> analysisResult){
        return """
                候选人简历：
                %s
                
                目标岗位：
                %s
                
                简历分析结果：
                重点关注方向：%s
                技能缺口：%s
                
                请生成面试题。
                """.formatted(
                        resumeText,
                        jdContent,
                        analysisResult.getOrDefault("interviewFocus",""),
                        analysisResult.getOrDefault("skillGaps","")
        );
    }

    public static String getAnswerFeedbackPrompt(){
        return """
                你是一个技术面试官，请评价候选人对面试问题的回答。
                从准确性、完整性、表达清晰度三个维度评价，给出改进建议。
    
                返回 JSON：
                {
                  "accuracyScore": 80,
                  "completenessScore": 70,
                  "clarityScore": 75,
                  "overallFeedback": "整体评价",
                  "improvements": ["改进建议1", "改进建议2"],
                  "followUp": "可能的追问"
                }
    
                只返回 JSON，不要包含其他文本。
                """;
    }

    public static String buildAnswerFeedbackMessage(String question, String answer){
        return """
                请评价以下面试问题与候选人回答。
                
                面试问题：
                %s
                
                候选人回答：
                %s
                """.formatted(question, answer);
    }
}
