export type Company = {
  id: string
  name: string
  ticker: string
}

export type PromiseSummary = {
  id: string
  statement: string
  category: string | null
  importance: 'HIGH' | 'MEDIUM' | 'LOW'
  timeframe: string | null
  status: string
  assessment: string
}

export type QuarterGroup = {
  fiscalYear: number
  fiscalQuarter: number
  transcriptId: string
  promises: PromiseSummary[]
}

export type CompanyLedger = {
  id: string
  name: string
  ticker: string
  quarters: QuarterGroup[]
}

export type EvidenceItem = {
  id: string
  sourceType: string
  text: string
  confidence: number | null
}

export type PromiseDetail = {
  id: string
  statement: string
  sourceQuote: string | null
  category: string | null
  importance: string
  timeframe: string | null
  status: string
  assessment: string
  speaker: string | null
  company: Company
  transcript: {
    id: string
    fiscalYear: number
    fiscalQuarter: number
  }
  evidence: EvidenceItem[]
}

export type ApiError = {
  detail?: string
  title?: string
  status?: number
}
