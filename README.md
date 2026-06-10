# promise-ledger

PromiseLedger MVP
Vision
Investors can easily access earnings call transcripts, financial statements, investor presentations, and
AI-generated summaries.
What they cannot easily access is:
• 
• 
• 
What management promised
Whether management delivered
How management credibility is changing over time
PromiseLedger is a management accountability platform for public companies.
It extracts measurable commitments from earnings call transcripts, tracks them across quarters,
verifies outcomes using financial statements and disclosures, and builds a historical management
credibility record.
Core Problem
Retail investors following 20–50 stocks do not have time to:
• 
• 
• 
• 
Read every earnings call transcript
Remember promises made 3 quarters ago
Verify if management delivered
Track changing management credibility
Institutional investors often perform this analysis manually.
PromiseLedger automates it.
Primary User
Indian equity investors.
Initially focused on:
• 
• 
• 
IT Services
Midcaps
Technology companies
Initial coverage:
• 
KPIT Technologies
1
• 
• 
• 
• 
Coforge
Newgen Software
Persistent Systems
LTIMindtree
MVP Objective
For each company:
1. 
2. 
3. 
4. 
5. 
6. 
7. 
8. 
9. 
10. 
Ingest transcript
Extract measurable promises
Store promises
Ingest future transcripts and financials
Determine status:
Open
Delivered
Partially Delivered
Missed
Display management credibility
What Is A Promise?
A promise must satisfy at least one of:
• 
• 
• 
Quantifiable
Time-bound
Verifiable
Examples:
GOOD
"Margins should improve in H2."
"We expect net debt to reduce by ₹500 crore."
"We will commission the new plant by Q4."
"We expect to hire 2,000 engineers."
BAD
2
"We remain optimistic."
"Demand environment looks healthy."
"We are confident."
"We believe our strategy is strong."
Promise Categories
Growth
• 
• 
• 
• 
Revenue Growth
Order Book Growth
Customer Growth
Geographic Expansion
Examples:
"Revenue growth should exceed industry growth."
"Order book should cross ₹5000 crore."
Profitability
• 
• 
• 
Margin Improvement
Cost Reduction
ROCE Improvement
Examples:
"EBITDA margin should improve 150bps."
Balance Sheet
• 
• 
• 
• 
Debt Reduction
Cash Growth
Reserve Growth
Working Capital Improvement
Examples:
"We expect to become net debt free."
"Cash balance should exceed ₹1000 crore."
3
Capital Allocation
• 
• 
• 
• 
Factory Construction
Capacity Expansion
Plant Commissioning
Capex Completion
Examples:
"New facility operational by Q4."
Operations
• 
• 
• 
Hiring Targets
Attrition Reduction
Capacity Utilization
Examples:
"We plan to hire 2000 engineers."
Products & Strategy
• 
• 
• 
Product Launches
Platform Rollouts
AI Initiatives
Examples:
"Our AI platform launches in H2."
Promise Importance
Each promise receives:
• 
• 
• 
HIGH
MEDIUM
LOW
Examples:
HIGH
• 
• 
• 
Debt reduction
Revenue guidance
Margin guidance
4
MEDIUM
• 
• 
LOW
• 
• 
Product launches
Customer additions
Small hiring targets
Regional office openings
Promise Lifecycle
States:
OPEN
Promise exists but deadline not reached.
PARTIAL
Progress observed but target not fully met.
DELIVERED
Target achieved.
MISSED
Deadline passed and target not achieved.
EXPIRED
Insufficient evidence after deadline.
Evidence Framework
PromiseLedger should not rely only on transcripts.
Evidence sources:
Financial Statements
Used for:
Revenue
5
• 
• 
• 
• 
• 
• 
Margins
Debt
Cash
Reserves
Working capital
Highest confidence source.
Investor Presentations
Used for:
• 
• 
• 
Order book
Customer additions
Capacity
Exchange Filings
Used for:
• 
• 
• 
Factory commissioning
Capex completion
Major announcements
Earnings Call Transcript
Used for:
• 
• 
Clarifications
Follow-up commentary
Lowest evidence priority.
Evidence Priority
1. 
2. 
3. 
4. 
Financial Statements
Exchange Filings
Investor Presentations
Earnings Calls
Numbers beat words.
6
Credibility Model V1
Delivered = 1.0
Partial = 0.5
Missed = 0
Formula:
Credibility Score = Earned Points / Total Possible Points
Example:
Delivered = 20
Partial = 4
Missed = 6
Score:
(20 + 4×0.5) / 30
= 73.3%
Recency Weighting
Historical performance matters.
Recent performance matters more.
Formula:
Recent 4 Quarters = 70%
Older History = 30%
Purpose:
• 
• 
• 
Adapt to management changes
Reflect turnarounds
Avoid punishing new leadership forever
7
Future Leadership Attribution
Not MVP.
Future version:
Track:
• 
• 
CEO
CFO
Measure:
• 
• 
Company Credibility
Current Management Credibility
Example:
Company Credibility: 67%
Current CEO Credibility: 91%
Trend: Improving
MVP Screens
Company Dashboard
KPIT
Credibility: 81%
Open Promises: 4
Delivered: 28
Missed: 5
Recent Trend: Improving
Promise Ledger
Promise
Quarter
Status
Margin improvement Q1 FY27 Delivered
8
Promise
Quarter
Status
Factory launch
Q2 FY27 Open
Debt reduction
Q3 FY27 Partial
Promise Detail Page
Promise:
Margins should improve in H2.
Status:
Delivered
Evidence:
EBITDA Margin
Q1 FY27: 18.1%
Q3 FY27: 20.4%
Confidence:
94%
Technical Architecture
Backend:
• 
• 
• 
• 
• 
Java 21
Spring Boot
PostgreSQL
Flyway
Docker
Frontend:
• 
• 
• 
AI:
React
TypeScript
Tailwind
Claude API
9
• 
Hosting:
• 
Railway / Render / AWS
Database Tables
Company
• 
• 
• 
id
ticker
name
Transcript
• 
• 
• 
• 
id
company_id
quarter
transcript_text
Promise
• 
• 
• 
• 
• 
• 
• 
• 
id
company_id
transcript_id
statement
category
importance
deadline
status
Evidence
• 
• 
• 
• 
• 
id
promise_id
source_type
evidence_text
confidence
CredibilitySnapshot
• 
• 
• 
• 
id
company_id
quarter
score
Claude Extraction Prompt
Read this earnings call transcript.
10
Extract ONLY measurable management commitments.
Ignore:
• 
• 
• 
optimism
confidence statements
generic commentary
Return JSON.
For each commitment provide:
• 
• 
• 
• 
• 
• 
statement
category
importance
timeframe
measurable
confidence
Return valid JSON only.
Claude Evaluation Prompt
Inputs:
1. 
2. 
3. 
Historical promise
Current quarter transcript
Current financial metrics
Determine:
• 
• 
• 
• 
Delivered
Partial
Missed
Open
Provide:
• 
• 
• 
• 
status
confidence
evidence
reasoning
Return JSON only.
11
MVP Success Criteria
For 5 companies:
• 
• 
• 
• 
Extract promises automatically
Track promises across quarters
Produce ledger
Classify status correctly at least 80% of the time
Do NOT build:
• 
• 
• 
• 
• 
• 
• 
Subscriptions
Payments
Watchlists
Alerts
Mobile app
Portfolio tracking
AI chat
The goal is proving management accountability tracking works before building a business around it.
12
