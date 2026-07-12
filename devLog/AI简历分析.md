# AI简历分析

## 当前阶段目标

- [ ] AI Client 封装
- [ ] Prompt 模板设计
- [ ] 分析任务创建（异步）
- [ ] 任务状态查询
- [ ] AI JSON 输出校验
- [ ] 分析结果存储

## 写 AI Client

与大模型对接的核心封装。  
[AiClient](../vitaelens-backend/src/main/java/com/phrolova/vitaelensbackend/ai/AiClient.java)

## 写 Prompt 模板

[PromptTemplate](../vitaelens-backend/src/main/java/com/phrolova/vitaelensbackend/ai/PromptTemplate.java)

## 写 AnalysisTask 实体和 Mapper

[AnalysisTask](../vitaelens-backend/src/main/java/com/phrolova/vitaelensbackend/entity/AnalysisTask.java)  
[AnalysisTaskMapper](../vitaelens-backend/src/main/java/com/phrolova/vitaelensbackend/mapper/AnalysisTaskMapper.java)

## 写 Hash 工具

用于缓存 key 生成。

[HashUtil](../vitaelens-backend/src/main/java/com/phrolova/vitaelensbackend/util/HashUtil.java)

## 写 AiCallLog 实体和 Mapper

[AiCallLog](../vitaelens-backend/src/main/java/com/phrolova/vitaelensbackend/entity/AiCallLog.java)  
[AiCallLogMapper](../vitaelens-backend/src/main/java/com/phrolova/vitaelensbackend/mapper/AiCallLogMapper.java)

## 写分析 Service

[AnalysisService](../vitaelens-backend/src/main/java/com/phrolova/vitaelensbackend/service/AnalysisService.java)  
[AnalysisServiceImpl](../vitaelens-backend/src/main/java/com/phrolova/vitaelensbackend/service/impl/AnalysisServiceImpl.java)

## 写 DTO 和 Controller

[CreateAnalysisRequest](../vitaelens-backend/src/main/java/com/phrolova/vitaelensbackend/dto/request/CreateAnalysisRequest.java)  
[TaskResponse](../vitaelens-backend/src/main/java/com/phrolova/vitaelensbackend/dto/response/TaskResponse.java)  

[AnalysisController](../vitaelens-backend/src/main/java/com/phrolova/vitaelensbackend/controller/AnalysisController.java)