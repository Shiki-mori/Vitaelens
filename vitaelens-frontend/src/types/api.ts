export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email?: string
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
}

export interface ResumeResponse {
  id: number
  fileName: string
  parsedText: string
  textLength: number
  createdAt: string
}

export interface CreateJobRequest {
  title: string
  content: string
}

export interface JobResponse {
  id: number
  title: string
  content: string
  createdAt: string
}

export interface CreateAnalysisRequest {
  resumeId: number
  jdId: number
}

export interface RewriteSuggestion {
  original: string
  suggested: string
  reason: string
}

export interface DimensionScores {
  technicalMatch?: number
  projectExperience?: number
  expressionQuality?: number
  jobMatch?: number
  [key: string]: number | undefined
}

export interface AnalysisResultJson {
  overallScore?: number
  dimensionScores?: DimensionScores
  strengths?: string[]
  weaknesses?: string[]
  skillGaps?: string[]
  rewriteSuggestions?: RewriteSuggestion[]
  interviewFocus?: string[]
  [key: string]: unknown
}

export type TaskStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED'

export interface TaskResponse {
  id?: number
  resumeId: number
  jdId: number
  status: TaskStatus | string
  score?: number | null
  resultJson?: AnalysisResultJson | null
  errorMessage?: string | null
  createdAt?: string
  finishedAt?: string | null
}
