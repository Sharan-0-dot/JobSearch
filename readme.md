# Job Search Agent — AI-Powered Intelligent Job Discovery Platform

An autonomous, production-grade AI platform that intelligently matches job seekers with opportunities through semantic understanding, deterministic ranking, and personalized career coaching.

---

## The Problem We Solve

Job seekers face a fundamental challenge:

- **Information Overload**: Thousands of irrelevant listings across multiple platforms
- **Poor Matching**: Algorithmic filtering misses genuinely suitable opportunities  
- **Resume Rejection**: No guidance on how to improve for specific roles
- **Time Inefficiency**: Manual screening of jobs wastes hours

**This platform solves it** by acting as an intelligent career advisor that understands both your profile and job opportunities at a semantic level—not just keyword matching.

---

## What Makes This Different

### 🧠 **Autonomous Intelligence, Not Just a Chatbot**

Unlike typical LLM wrappers, this is a complete **agentic system** with:

- **Planning Agent**: Analyzes your query and decides which tools to invoke and in what order
- **Executor Tools**: Performs deterministic job search, ranking, and analysis
- **Synthesizer Agent**: Generates personalized explanations with reasoning
- **Memory Layer**: Maintains multi-turn conversation context with PostgreSQL backing

### 📊 **Hybrid Ranking — Transparency + Intelligence**

Jobs are ranked using a **two-tier system**:

1. **Deterministic Scoring (35% skill overlap, 20% location, 15% experience, 20% tech stack, 10% salary)**
   - Reproducible, explainable, no hallucinations
   - Based on semantic embeddings + user profile matching

2. **LLM Explanation Layer**
   - Why does this job match you?
   - What skills are missing?
   - What should you improve?

This architecture ensures **consistency + personalization**.

### 🎯 **Resume Analysis Engine**

Not just job search—active career improvement:

- **Gap Analysis**: Identifies missing skills for target roles
- **ATS Optimization**: Suggests keywords to improve resume visibility
- **Coaching Feedback**: Recommends specific projects and improvements
- **Skill Extraction**: Auto-parses resume to understand your profile structurally

### 🔍 **Semantic Intelligence via Vector Embeddings**

Resume and jobs are converted to **768-dimensional vectors** using embeddings:

- Resume: `[0.234, -0.882, 0.551, ...]` → encodes your career DNA semantically
- Jobs: Similar encoding → enables true semantic matching

Even when a job says "Java microservices" and you say "Spring Boot APIs", the system understands they're related.

### 🛡️ **Production-Grade Resilience**

Built for reliability:

- **Circuit Breaker Pattern**: Gracefully handles API failures
- **Retry Logic**: Exponential backoff for transient errors
- **Fallback Caching**: Returns cached jobs if live API fails
- **Link Validation**: Ensures apply URLs are safe and valid

---

## Key Features

### Job Search & Discovery
- **Live Job Fetching**: Real-time integration with JSearch API (LinkedIn, Indeed, Glassdoor)
- **Deduplication**: Eliminates duplicate listings via intelligent hashing
- **Semantic Ranking**: Finds jobs that match you semantically, not just keyword-matching
- **Top-K Retrieval**: Returns best 5-10 matches with detailed explanations

### Resume Intelligence  
- **PDF/DOCX Parsing**: Extracts text from resumes using Apache Tika
- **Skill Extraction**: Automatically identifies technical & soft skills
- **Profile Building**: Stores your skills, experience, and preferences
- **Personalization**: Learns from your feedback to improve recommendations

### Match Analysis
- **Resume-Job Comparison**: Detailed analysis of how well you fit each role
- **Missing Skills Detection**: Identifies gaps between your profile and job requirements
- **Salary Fit Analysis**: Checks alignment with your expectations
- **ATS Feedback**: Suggests resume improvements for better keyword matching

### Execution Transparency
- **Trace Logs**: Full visibility into agent reasoning and tool invocation
- **Observability**: Request metrics, latency tracking, token usage monitoring
- **Debugging Support**: Understand exactly why the agent recommended a job
- **Audit Trail**: Complete history of searches, analyses, and feedback

### Feedback Loop
- **Engagement Tracking**: Record which jobs you liked/applied for
- **Preference Learning**: System improves rankings based on your feedback
- **Conversation Memory**: Multi-turn context across sessions
- **Reinforcement**: Your engagement data trains personalization

---

## Technical Architecture

### Core Stack

| Component | Technology              | Purpose |
|-----------|-------------------------|---------|
| **Backend** | Spring Boot 3.x         | REST API, dependency injection, configuration |
| **AI Agent Framework** | LangChain4j 0.36+       | Orchestration, tool binding, memory management |
| **LLM (Local)** | Llama 3 via Ollama      | Fast development iteration, no API costs |
| **LLM (Cloud)** | Gemini 2.5 flash        | Production scalability (single bean swap) |
| **Vector Database** | PostgreSQL + pgvector   | Semantic search with 768-dim embeddings |
| **Job Data** | JSearch API             | Aggregated listings from LinkedIn, Indeed, Glassdoor |
| **Resilience** | Resilience4j            | Circuit breaker, retry, timeout policies |
| **Resume Parsing** | Apache Tika + PDFBox    | PDF/DOCX text extraction |
| **Observability** | Micrometer + Prometheus | Metrics, latency tracking, distributed tracing |

### Architecture Pattern

```
User Query
    ↓
[Planner Agent] — Decides which tools to invoke
    ↓
[Tool Orchestration]
  ├─ JSearchTool (fetch live jobs + dedup)
  ├─ JobRagTool (semantic search + hybrid ranking)
  ├─ ResumeOptimizationTool (gap analysis)
  ├─ UserProfileTool (personalization context)
  └─ SkillExtractorTool (profile understanding)
    ↓
[Deterministic Ranking] — Transparent scoring
    ↓
[Executor/Synthesizer] — LLM explanation layer
    ↓
[Response] — Jobs + scoring breakdown + personalized coaching
```

### Database Intelligence

Multi-table PostgreSQL design:

- **job_listings** — Live job data with metadata
- **job_embeddings** — 768-dim vectors for semantic search  
- **user_profiles** — Your skills, experience, preferences
- **resume_analysis** — Match scores + feedback for each job
- **agent_memory** — Multi-turn conversation context
- **agent_execution_trace** — Full audit trail of agent reasoning
- **job_feedback** — Your engagement signals (liked, applied, matched)
- **search_history** — Query logs for personalization

---

## Use Cases

### For Job Seekers

**"Find backend internships in Bangalore with Spring Boot"**

→ System fetches 100+ live jobs, deduplicates to 45 unique, semantically ranks top 5, explains why each matches your profile, and suggests resume improvements.

**"Analyze my resume for this role"**

→ System compares your resume to job requirements, identifies missing skills (Redis, Docker Compose), suggests ATS keywords, recommends specific projects to highlight.

**"What jobs did I like?"**

→ System reviews your feedback history, identifies patterns (you prefer backend + microservices + remote), and personalizes future rankings.

### For Capstone/Portfolio

This project demonstrates:

- **Autonomous AI Architecture**: Planner-Executor with ReAct loops
- **Production Engineering**: Resilience, observability, input validation
- **Data Intelligence**: Semantic embeddings + deterministic ranking hybrid
- **Resume-Grade Work**: Professional patterns (Spring Boot 3, Java 21, Docker, pgvector)
- **Full-Stack Backend**: REST APIs + database + LLM orchestration

---

## API Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/api/agent/query` | Main entry point — natural language job search |
| `GET` | `/api/jobs` | List cached jobs with match scores |
| `GET` | `/api/jobs/{id}` | Job details with scoring breakdown |
| `POST` | `/api/resume/upload` | Upload resume for extraction |
| `POST` | `/api/resume/analyze/{jobId}` | Analyze resume vs specific job |
| `GET` | `/api/profile/{userId}` | Get user profile + extracted skills |
| `POST` | `/api/feedback` | Record user engagement (liked/applied) |
| `GET` | `/api/trace/{traceId}` | View agent execution trace (debugging) |

### Example Response

```json
{
  "userId": "sharan-001",
  "response": "I found 5 backend internships matching your profile...",
  "jobs": [
    {
      "id": "uuid-1",
      "title": "Backend Intern - Spring Boot",
      "company": "Acme Corp",
      "location": "Bangalore",
      "matchScore": 91,
      "scoringBreakdown": {
        "skillOverlap": 0.95,
        "locationMatch": 1.0,
        "experienceMatch": 0.85,
        "techStackMatch": 0.92,
        "salaryMatch": 0.78
      },
      "whyMatched": ["Spring Boot", "Kafka", "PostgreSQL"],
      "missingSkills": ["Redis"],
      "suggestedImprovements": ["Highlight Kafka project", "Add Docker keywords"]
    }
  ],
  "toolsUsed": ["JSearchTool", "JobRagTool"],
  "executionTimeMs": 2340
}
```

---

## Why This Project Stands Out

### 1. **Not Just "AI Wrapper"**

Most students wrap an LLM API. This is a complete **intelligent system** with:
- Multi-agent orchestration
- Deterministic + ML hybrid architecture
- Tool invocation framework
- Memory & reasoning layers

### 2. **Production-Grade Thinking**

- Resilience patterns (circuit breaker, retry, fallback)
- Observability from day one (metrics, tracing, logging)
- Input validation & safety guardrails
- Database indexing for scale (pgvector IVFFlat)

### 3. **Real Job Matching Intelligence**

- Embeddings capture semantic meaning (not just keywords)
- Deterministic ranking ensures reproducibility
- Resume analysis adds personalization value
- Feedback loop enables continuous improvement

### 4. **Complete Backend Competence**

- Spring Boot 3.x best practices
- Database design (normalization, indexing, constraints)
- API design (REST conventions, pagination, error handling)
- Docker containerization
- Configuration management (profiles, environment variables)

### 5. **Interview-Ready**

Clear talking points:

✅ "I built an autonomous agentic system, not just an LLM wrapper"

✅ "The ranking system is deterministic + explainable, avoiding LLM hallucinations"

✅ "Resume analysis engine provides personalized career coaching"

✅ "Resilience patterns ensure the system survives API failures"

✅ "Complete observability layer tracks agent reasoning"

✅ "Semantic embeddings + vector search for intelligent matching"

---

## Technical Highlights

### Semantic Search with pgvector

```sql
-- Resume embedding vs job embeddings
SELECT job_id, 1 - (resume_embedding <=> job_embedding) as similarity
FROM job_embeddings
ORDER BY similarity DESC
LIMIT 5;
```

Uses **768-dimensional vectors** for deep semantic understanding.

### Deterministic Hybrid Ranking

```
Final Score = 
  0.35 * skill_overlap +
  0.20 * location_match +
  0.15 * experience_match +
  0.20 * tech_stack_match +
  0.10 * salary_match
```

Reproducible, explainable, no randomness.

### Resilience Architecture

```
JSearch API Call
  ↓
[Circuit Breaker] — Catches failures
  ↓ (if open)
[Fallback] — Return cached jobs
  ↓ (if half-open)
[Retry Logic] — Exponential backoff
```

Guarantees user experience even when APIs fail.

### Execution Tracing

Full audit trail:
- Planning phase reasoning
- Tools invoked + execution time
- Observations from each tool
- Final synthesis

Enables debugging and transparency.

---

## Impact

**For Users**: Saves hours of job searching with intelligent, personalized recommendations + career coaching.

**For Teams**: Demonstrates production-grade backend engineering with autonomous AI systems.

**For Interviews**: Clear evidence of:
- Advanced system design (agents, tools, memory)
- Production thinking (resilience, observability, safety)
- Data intelligence (embeddings, ranking, personalization)
- Backend mastery (Spring Boot, PostgreSQL, Docker)

---

## The Bottom Line

This isn't just another job search site or AI chatbot.

It's a **complete intelligent platform** that:

1. **Understands you** — via resume parsing & semantic embeddings
2. **Understands opportunities** — via live job aggregation & embedding
3. **Matches intelligently** — deterministic ranking + LLM explanation
4. **Helps you improve** — resume analysis + ATS optimization
5. **Learns over time** — feedback loop + personalization

All built with **production-grade architecture**, **observability**, and **resilience**.

This is the kind of system senior engineers build at job marketplaces, career platforms, and recruitment tech companies.

