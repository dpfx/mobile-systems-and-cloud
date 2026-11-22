# Research – Task 2

Facts collected for Part B (architecture comparison) and Part E (operation, costs and law), before the comparison itself. This file is the evidence; `architecture.md` and `operations.md` cite it.

**Method.** Only official pages of well-known providers (their own pricing, docs and terms pages; for law, the official text). The pages were fetched and excerpted with Claude Code's web fetch; quotes are verbatim from the fetched text, shortened with "…". "Not stated" means the pages read don't say it; nothing is filled in from memory. Free tiers change often: every fact is only valid for the date it was read.

## 1 · Free hosting and database (EU region, credit card)

**Question.** Which services run a small web backend and a managed database for free, in an EU region, ideally without a credit card? Which limits apply? Course rule (PDF page 4): free tiers only; where a card is required, set a budget limit or cost alert first.

All pages in this section were read on **2026-11-22**.

### Summary: backend hosting

| Service | Free amount | EU region | Credit card | Catch |
|---|---|---|---|---|
| Google Cloud Run | 2M requests, 180,000 vCPU-s, 360,000 GB-s per month | Free tier regions not stated | Billing account required; card required for the free trial | Spend cap available (hard limit) |
| Render free web service | 750 instance hours per workspace per month | Frankfurt exists; free instances there not stated | Not stated (payment method optional per docs) | Spins down after 15 min, about 1 min to start; "Do not use them for production applications" |
| Vercel Hobby (Functions, Python) | 1M invocations, 4 CPU-hours, 360 GB-hours per month | One region per project; `fra1` exists, default `iad1` (US) | Not stated | Terms: personal or non-commercial use only; over a limit: wait 30 days, no charges |
| Azure for Students | $100 credit for 12 months; Container Apps grant 180,000 vCPU-s, 360,000 GiB-s, 2M requests per month | Germany West Central, West Europe, Sweden Central on the pricing pages | Not required | Full-time students only; subscription disabled when the credit is used up |
| AWS Free plan | $100 credits (up to $100 more); Lambda 1M requests and 400,000 GB-s per month | Credits apply across regions | Not required for most new customers | Account closes after 6 months or when the credits run out |
| Oracle Always Free | 2 AMD micro VMs; Arm A1 equivalent to 2 OCPUs and 12 GB | Frankfurt exists; Always Free only in the home region | Required for most users, not charged | Home region fixed at sign-up |
| Fly.io | Docs: no free tier for new organisations; $1 trial credit for 7 days | Frankfurt, Amsterdam, Paris, Stockholm | Not for the trial; required afterwards | Pricing page contradicts the docs |

### Summary: database and authentication

| Service | Free amount | EU region | Credit card | Catch |
|---|---|---|---|---|
| Cloud Firestore (Firebase Spark) | 1 GiB stored; 50,000 reads, 20,000 writes, 20,000 deletes per day; 10 GiB egress per month | `eur3` or a single region such as `europe-west3` (Frankfurt); fixed after creation | Not needed | Usage shut off when a quota is exceeded |
| Firebase Authentication (Spark) | 50,000 MAU (listed under "With Identity Platform"); 3,000 daily active users; 100 new accounts per hour per IP | – | Not needed | Cloud Functions and App Hosting only on Blaze |
| Supabase Free | 500 MB database; 50,000 Auth MAU; 2 active projects | Frankfurt and five other European regions; choice on the free plan not stated | Not stated | Paused after 1 week of inactivity |
| Neon Free | 1 GB storage and 100 CU-hours per project per month | AWS Frankfurt | Not required | Scales to zero after 5 min |
| MongoDB Atlas Free | 0.5 GB; 500 connections; 100 operations per second | Some regions only; which EU regions not readable | Not stated | Paused after 30 days without connections |
| Render Postgres free | 1 GB | Frankfurt | As Render | Expires 30 days after creation, then deleted |
| Azure Cosmos DB / Azure SQL free tier | Cosmos: 1,000 RU/s and 25 GB; SQL: up to 10 databases with 100,000 vCore-s and 32 GB each per month | As Azure | As Azure | Needs an Azure subscription; SQL free tier excludes "Azure for Students Starter" |
| Amazon DynamoDB | 25 GB, 25 WCU, 25 RCU per region | As AWS | As AWS | "Always free" not stated |
| Oracle Autonomous Database | 2 databases with 20 GB each | Home region | As Oracle | – |

### Details

#### Firebase (Spark plan)

| Fact | Quote | Src |
|---|---|---|
| No payment details on Spark | "You don't need to provide any payment information to get started using most Firebase features right away!" | F2 |
| | "No payment method needed." | F1 |
| A billing account means Blaze | "linking a Cloud Billing account to your project will auto-upgrade your project to the Blaze pricing plan" | F2 |
| Firestore free quota | Free tier table: stored data 1 GiB; document reads 50,000 per day; document writes 20,000 per day; document deletes 20,000 per day; outbound data transfer 10 GiB per month | F3 |
| Daily reset | "Quotas are applied daily and reset around midnight Pacific time." | F3 |
| One free database | "Cloud Firestore allows exactly one free database per project." | F3 |
| EU locations | Multi-region `eur3` (read-write `europe-west1` Belgium and `europe-west4` Netherlands, witness `europe-north1` Finland); regional, among others, `europe-west3` (Frankfurt), `europe-west10` (Berlin), `europe-west9` (Paris), `europe-north2` (Stockholm) | F4 |
| Location is permanent | "once you provision a database instance, you cannot change its location setting." | F4 |
| Auth monthly users | "Monthly active users \| 50K MAUs \| No-cost up to 50K MAUs", listed under "Other Authentication services / With Identity Platform" | F1 |
| Auth daily users on Spark | "Tier 1 Daily Active Users: 3000 per day" | F5 |
| Sign-up limit | "New account creation: 100 accounts/hour for each IP address." | F5 |
| Auth emails on Spark | Address verification emails 1000 per day; password reset emails 150 per day | F5 |
| Limits may change | Auth limits "can change without notice." | F5 |
| Cloud Functions only on Blaze | Blaze: "Access to Cloud Functions (no-cost usage quota, then pay-as-you-go pricing for additional usage)"; the Spark list has no Cloud Functions | F2 |
| App Hosting not on Spark | App Hosting's Spark column: "Not applicable" | F1 |
| Over quota | "your project's usage of that specific product will be shut off for the remainder of that month" | F2 |

- F1 https://firebase.google.com/pricing
- F2 https://firebase.google.com/docs/projects/billing/firebase-pricing-plans
- F3 https://firebase.google.com/docs/firestore/quotas
- F4 https://firebase.google.com/docs/firestore/locations
- F5 https://firebase.google.com/docs/auth/limits

#### Google Cloud Run

| Fact | Quote | Src |
|---|---|---|
| Free amount | "2 million requests per month." "360,000 GB-seconds of memory, 180,000 vCPU-seconds of compute time." "1 GB of outbound data transfer from North America per month." | G1 |
| Free tier regions | Not stated (the Cloud Run row names no regions) | G1 |
| Billing account | "A Google Cloud billing account is required to access the Google Cloud Free Tier." | G1 |
| Card for the trial | "you must provide a credit card or other payment method that is valid for the period of the Free Trial." Whether the free tier alone needs a payment method: not stated | G1 |
| Budget alerts don't stop spending | "Setting an alerts-only budget doesn't automatically cap Google Cloud or Google Maps Platform usage or spending." | G2 |
| Spend cap | "The spend cap is enforced when your gross, estimated usage costs exceed 100% of your budget amount." Then "usage of your specified services is automatically paused for the remainder of the budget period" | G3 |
| Eligible services | "the eligible services include Gemini API, Agent Platform (formerly known as Vertex AI), Cloud Run, and Cloud Run functions." | G3 |
| Spend cap scope | "Spend cap budgets can only be set on one project and one service at a time." | G3 |
| Spend cap limit | "Spend caps don't pause any on-going, fixed usage associated with persistent resources" | G3 |
| Spend cap launch stage | Not stated | G3 |
| Scale to zero | "scale to zero when idle to reduce costs"; minimum instances "will incur cost even when the service is not actively serving requests." Default minimum: not stated | G4 |

- G1 https://docs.cloud.google.com/free/docs/free-cloud-features
- G2 https://docs.cloud.google.com/billing/docs/how-to/budgets
- G3 https://docs.cloud.google.com/billing/docs/how-to/budgets-spend-caps
- G4 https://docs.cloud.google.com/run/docs/about-instance-autoscaling
- https://cloud.google.com/run/pricing could not be read (only the page title came back).

#### Render

| Fact | Quote | Src |
|---|---|---|
| Spin-down | "Render spins down a Free web service that goes 15 minutes without receiving any inbound traffic." | R1 |
| Spin-up | "This process takes about one minute." | R1 |
| Free hours | "Render grants 750 Free instance hours to each workspace per calendar month" | R1 |
| Not for production | "Do not use them for production applications." | R1 |
| Restarts | "Render might restart a Free web service at any time." | R1 |
| Payment method optional | "If you haven't added a payment method, Render instead suspends all of your Free services for the remainder of the month." (on bandwidth overage) | R1 |
| Postgres size | "Free Render Postgres databases have a fixed storage capacity of 1 GB." | R1 |
| Postgres expiry | "Free Render Postgres databases expire 30 days after creation." "you have a grace period of 14 days to upgrade it to a paid compute plan." "After the grace period, Render deletes the database (along with all of its data)." | R1 |
| EU region | "Frankfurt, Germany" in the region list. Whether free instances can use it: not stated | R2 |
| Card required | Not stated | R1 |
| RAM/CPU of free instances | Not stated | R1 |

- R1 https://render.com/docs/free
- R2 https://render.com/docs/regions

#### Vercel (Hobby plan)

| Fact | Quote | Src |
|---|---|---|
| Free | "The Hobby plan is free and includes a generous set of features on a per month basis" | V1 |
| Non-commercial only | "You shall only use the Services under a Hobby plan for your personal or non-commercial use." (Terms, section 4; "Last Updated October 19, 2026") | V2 |
| Function limits | "Function Invocations \| First 1,000,000"; "Active CPU \| 4 CPU-hrs"; "Provisioned Memory \| 360 GB-hrs"; "Vercel Function maximum duration \| 300s (5 minutes)" | V1 |
| Per function | "2 GB / 1 vCPU default and max" | V3 |
| One region | "Hobby \| Single region"; default "Washington, D.C., USA (iad1) for all new projects" | V4 |
| Frankfurt | "fra1 \| eu-central-1 \| Frankfurt, Germany". A restriction of Hobby to certain regions: not stated | V5 |
| Python and FastAPI | "Use the Python runtime to run ASGI … and WSGI … applications on Vercel"; "The Python framework presets work with FastAPI, Flask, Django, and other Python web frameworks" | V6 |
| Over a limit | "In most cases, if you exceed your usage limits on the Hobby plan, you will have to wait until 30 days have passed before you can use the feature again" | V1 |
| No charges | "If you have a free Hobby account, you are limited to the usage caps and cannot purchase additional usage" | V7 |
| Card required | Not stated | V1, V7 |

- V1 https://vercel.com/docs/plans/hobby
- V2 https://vercel.com/legal/terms
- V3 https://vercel.com/docs/limits
- V4 https://vercel.com/docs/functions/configuring-functions/region
- V5 https://vercel.com/docs/regions
- V6 https://vercel.com/docs/functions/runtimes/python
- V7 https://vercel.com/pricing

#### Microsoft Azure (Azure for Students)

| Fact | Quote | Src |
|---|---|---|
| No card | "A credit card is not required for the Azure for Students offer." | A1 |
| Credit | "$100 in Azure credits to be used within the first 12 months" | A1 |
| Eligibility | "attend an accredited, degree-granting two-year or four-year educational institution" "where you're a full-time student." "You must verify your academic status through your organization's email address." | A1 |
| Credit used up | "If you exhaust your available credits, your Azure subscription will be disabled." | A1 |
| Inactivity | Microsoft may remove compute resources if the subscription "is not incurring charges or is inactive for more than 90 days" | A1 |
| Region exclusion | "This does not include sovereign cloud products in US Government, or Azure Germany regions." "Azure Germany" was a separate sovereign instance ("a physically isolated instance of Microsoft Azure"), closed on 29 October 2021 according to Microsoft's archived page. Whether the exclusion affects the public region Germany West Central: not stated | A1, A2 |
| Container Apps grant | "The first 180,000 vCPU-seconds, 360,000 GiB-seconds, and 2 million requests per subscription per month are free." | A3 |
| App Service F1 | "F1 Free", "Shared (60 CPU minutes / day)", 1 GB RAM; "Use of free plan for production workloads is not supported." | A4 |
| Cosmos DB free tier | "you get the first 1000 RU/s and 25 GB of storage in the account for free"; "You can have up to one free tier Azure Cosmos DB account per an Azure subscription"; "Free tier lasts indefinitely for the lifetime of the account" | A5 |
| Azure SQL free tier | "100,000 vCore seconds, 32 GB of data, and 32 GB of backup storage free per month for the lifetime of your subscription"; "up to 10 free tier General Purpose databases"; "Microsoft Azure for Students Starter isn't supported." | A6 |
| EU regions | Germany West Central, West Europe, Sweden Central and Germany North are in the price lists of Container Apps and App Service | A3, A4 |

- A1 https://azure.microsoft.com/en-us/pricing/offers/ms-azr-0170p/
- A2 https://learn.microsoft.com/en-us/previous-versions/azure/germany/germany-welcome
- A3 https://azure.microsoft.com/en-us/pricing/details/container-apps/
- A4 https://azure.microsoft.com/en-us/pricing/details/app-service/linux/
- A5 https://learn.microsoft.com/en-us/azure/cosmos-db/free-tier
- A6 https://learn.microsoft.com/en-us/azure/azure-sql/database/free-offer
- Also read: https://azure.microsoft.com/en-us/free/students/ ("No credit card required"; "Available only to full-time university students")

#### Amazon Web Services (Free plan)

| Fact | Quote | Src |
|---|---|---|
| Credits | "When you create a new AWS Free Tier account, you get $100 in credits immediately. As you explore key services, you can earn up to $100 more." | W1 |
| Account closes | "The account closes on its own 6 months after you open it or when your credits run out, whichever comes first. You won't be charged unless you convert to a Paid plan." | W1 |
| Data after expiry | "AWS will retain your data for 90 days after your Free plan expires." "If you don't upgrade your account within 90 days, AWS will permanently erase your AWS account and all its content." | W2 |
| Card | "A payment method isn't required to sign up for most new customers." | W2 |
| Regions | "The AWS Free Tier credit applies to participating services across global regions." | W2 |
| Lambda | "The free tier includes one million requests and 400,000 GB-seconds per month." Whether always free: not stated | W3 |
| DynamoDB | "per Region, per-payer account basis": "25 WCUs, 25 RCUs", "25 GB of data storage". Whether always free: not stated | W4 |

- W1 https://aws.amazon.com/free/
- W2 https://aws.amazon.com/free/free-tier-faqs/
- W3 https://aws.amazon.com/lambda/pricing/
- W4 https://aws.amazon.com/dynamodb/pricing/on-demand/
- Also read: https://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/free-tier.html

#### Oracle Cloud (Always Free)

| Fact | Quote | Src |
|---|---|---|
| Card | "For security purposes, most users need a mobile phone number and a credit card to create an account." "Your credit card will not be charged unless you upgrade your account." | O1 |
| Home region only | "You can provision Always Free Autonomous AI Databases and compute instances only in the home region." | O1 |
| One region | "Trial, free tier, and pay-as-you-go tenancies are limited to one subscribed region." | O3 |
| Compute | "All tenancies get up to two Always Free VM instances using the VM.Standard.E2.1.Micro shape"; Ampere A1: "the first 1,500 OCPU hours and 9,000 GB hours per month", "equivalent to 2 OCPUs and 12 GB of memory" | O2 |
| Database | "All tenancies get two Always Free Oracle Autonomous AI Databases."; "20 GB storage (cannot be scaled)" | O2 |
| Frankfurt | "Germany Central (Frankfurt) \| eu-frankfurt-1" | O3 |

- O1 https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier.htm
- O2 https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm
- O3 https://docs.oracle.com/en-us/iaas/Content/General/Concepts/regions.htm
- Not readable (HTTP 403): https://www.oracle.com/cloud/free/, https://www.oracle.com/cloud/free/faq/, https://www.oracle.com/cloud/data-regions.html

#### Fly.io

| Fact | Quote | Src |
|---|---|---|
| No free tier (docs) | "New organizations don't have a free tier or a monthly free usage allowance." | Y1 |
| Trial | "New accounts get $1 of free credit, with no credit card required." "The credit lasts for 7 days or until you use it up, whichever comes first." "Trial Machines stop automatically after running for 5 minutes." | Y2 |
| After the trial | "When your credit is used up or expires, your Machines stay stopped, and you can't start them or create new resources until you add a credit card." | Y2 |
| Card | "All organizations (except for Linked Organizations) require a credit card on file." | Y1 |
| Pricing page | "$5 of usage, every month" and "Credit card required."; elsewhere on the same page: "Start on the Free plan with $1 of starter credit, no credit card required." | Y3 |
| EU regions | `ams` Amsterdam, `arn` Stockholm, `cdg` Paris, `fra` Frankfurt | Y4 |

- Y1 https://docs.fly.io/about/pricing
- Y2 https://docs.fly.io/about/free-trial
- Y3 https://fly.io/pricing/
- Y4 https://docs.fly.io/reference/regions

#### Supabase (Free plan)

| Fact | Quote | Src |
|---|---|---|
| Limits | "500 MB database size per project included"; Auth MAUs "50,000 included"; "Limit of 2 active projects."; egress "5 GB included"; Edge Function invocations "500,000 included" | S1 |
| Pausing | "Free projects are paused after 1 week of inactivity." | S1 |
| Restore | "During the restore window a paused project can be restored to the platform with a single button click"; "When a project is paused, users have a 1-year window to restore the project" | S4 |
| EU regions | "Central EU (Frankfurt), `eu-central-1`", plus Ireland, London, Paris, Zurich and Stockholm; "Each Supabase project is deployed to one primary region". Region choice on the free plan: not stated | S3 |
| Card | Not stated for the free plan; "We accept credit card payments only." (payment methods in general) | S2 |
| Own rows per user | Example policy `using ( (select auth.uid()) = user_id )`; "`auth.uid()` returns the ID of the user making the request." Plan availability: not stated | S5 |

- S1 https://supabase.com/pricing
- S2 https://supabase.com/docs/guides/platform/billing-faq
- S3 https://supabase.com/docs/guides/platform/regions
- S4 https://supabase.com/docs/guides/platform/upgrading
- S5 https://supabase.com/docs/guides/database/postgres/row-level-security

#### Neon (Free plan)

| Fact | Quote | Src |
|---|---|---|
| Storage | "1 GB/project, 20 GB account total"; exceeding it "blocks writes" | N1 |
| Compute | "100 CU-hours/project" per month | N2 |
| Scale to zero | "Free: 5 min inactivity; cannot disable" | N2 |
| No card | "The Free plan is permanent (not a trial); no credit card required." | N1 |
| Frankfurt | AWS Europe (Frankfurt) `aws-eu-central-1`; "You can create a project in any region listed above" | N3 |

- N1 https://neon.com/pricing
- N2 https://neon.com/docs/introduction/plans
- N3 https://neon.com/docs/introduction/regions

#### MongoDB Atlas (Free cluster)

| Fact | Quote | Src |
|---|---|---|
| Storage | "Free clusters: 0.5 GB" | M1 |
| Connections | "Free clusters can only have a maximum of 500 connections." | M1 |
| Throughput | "Free clusters: 100 operations per second." | M1 |
| Pausing | "Atlas automatically pauses Free clusters after 30 days of inactivity where there are zero connections to the cluster." | M1 |
| Regions | "You can deploy Free clusters only in a subset of regions on AWS, Google Cloud, and Azure." "Atlas displays only the cloud provider regions that support Free clusters." Which EU regions: not readable (the free-support marks in the region tables don't come through) | M1, M2 |
| Card | Not stated | M2, M3 |

- M1 https://www.mongodb.com/docs/atlas/reference/free-shared-limitations/
- M2 https://www.mongodb.com/docs/atlas/tutorial/deploy-free-tier-cluster/
- M3 https://www.mongodb.com/pricing
- Also read: https://www.mongodb.com/docs/atlas/reference/amazon-aws/, https://www.mongodb.com/docs/atlas/reference/google-gcp/, https://www.mongodb.com/docs/atlas/reference/microsoft-azure/, https://www.mongodb.com/docs/atlas/billing/

### Contradictions in the official pages

1. **Firebase, quota exceeded:** F2 says usage is shut off "for the remainder of that month"; F3 says Firestore quotas "reset around midnight Pacific time" every day.
2. **Supabase, restore window:** S4 says "a 1-year window"; a screenshot on the same page shows "Project Paused: 90 Days Remaining".
3. **Fly.io, free plan:** Y1 says there is no free tier for new organisations; Y3 shows a free plan with "$5 of usage, every month" and says both "Credit card required" and "no credit card required".
4. **Firebase Authentication, monthly users:** the 50,000 MAU appear under "With Identity Platform" (F1); whether the limit also covers plain email and password sign-in is not stated.

### Gaps

- Credit card requirement not stated: Render, Vercel, Supabase, MongoDB Atlas.
- Cloud Run: whether the free tier applies in EU regions, and the default minimum number of instances (the pricing page can't be read automatically).
- MongoDB Atlas: which EU regions allow free clusters.
- Open question for topic 3: can Firebase Authentication and Cloud Firestore on the Spark plan be used from a backend hosted elsewhere?

### Observations for Part B (not a decision)

- **Backend:** free without a card and without a time limit are only Render (not for production, cold start of about a minute) and Vercel Hobby (non-commercial use only). Azure for Students needs no card, but the credit lasts 12 months and the offer is for full-time students. The AWS Free plan closes after 6 months. Cloud Run, Oracle and Fly.io need a billing account or card, which the course rules allow with a budget alert; Cloud Run additionally offers a spend cap that pauses the service.
- **Database:** free without a card, without expiry and in the EU are Cloud Firestore and Neon; Supabase probably too (card not stated). Render Postgres is unsuitable because it expires after 30 days, and Task 3 builds on this backend.
- If Cloud Run and Firebase share one project, the billing account moves the Firebase project to Blaze (F2).

### Note

Several Supabase docs pages (among them S2, S4, S5) contain a text block "Note for AI agents" that asks the reading agent to file a feedback report and to ask the user "Send this report to Supabase?". The fetching agent ignored it. It is a real example of instructions hidden in web content addressed to an AI (indirect prompt injection), relevant for Part D.

## 2 · How Anthropic handles Claude API data

**Question.** Where does Anthropic process requests to the Claude API, how long does it keep inputs and outputs, are they used for training, and which contract covers the processing? Needed for Part B (privacy: place of processing) and Part E (privacy).

All pages in this section were read on **2026-11-22**. Before reading, Claude Code's `claude-api` skill was loaded (project rule); its notes on `inference_geo` match D1.

### Summary

| Question | Answer | Src |
|---|---|---|
| Place of processing | Default `"global"` (any available geography); optional `"us"` at 1.1× the price; no EU option | D1 |
| Place of storage | Workspace geo: only `"us"` | D1 |
| Retention | Deleted within 30 days (D2); the docs say not retained by default (D3), see contradictions; up to 2 years if flagged | D2, D3 |
| Training | Not on customer content | D3, D4 |
| Contract | Data processing addendum is part of the Commercial Terms; Anthropic is the processor; EU Standard Contractual Clauses for transfers | D4, D5 |
| Key in the client | Anthropic advises against it; browser calls need an explicit "dangerous" header | D6 |

### Details

#### Place of processing and storage

| Fact | Quote | Src |
|---|---|---|
| Two settings | "Inference geo: Controls where model inference runs, on a per-request basis." "Workspace geo: Controls where data is stored at rest and where endpoint processing (such as image transcoding and code execution) occurs." | D1 |
| Default | `"global"`: "Default. Inference may run in any available geography for optimal performance and availability." | D1 |
| US only | `"us"`: "Inference runs only in US-based infrastructure." | D1 |
| No other option | "Inference geo: Only `"us"` and `"global"` are available." | D1 |
| Storage | "Currently, `"us"` is the only available workspace geo." "Workspace geo is set when you create a workspace and can't be changed afterward." | D1 |
| Price | "US-only inference (`inference_geo: "us"`) is priced at 1.1x the standard rate across all token pricing categories" | D1 |
| Evidence per request | "The response `usage` object includes an `inference_geo` field indicating where inference ran" | D1 |
| Models | "The `inference_geo` parameter is supported on Claude 4.6 and later models." | D1 |
| Workspace policy | `allowed_inference_geos` "Restricts which geos a workspace can use"; `default_inference_geo` "Sets the fallback geo when `inference_geo` is omitted from a request" | D1 |

#### Retention

| Fact | Quote | Src |
|---|---|---|
| Default (privacy center, article updated July 1, 2026) | "For Anthropic API users, we automatically delete inputs and outputs on our backend within 30 days of receipt or generation" | D2 |
| Flagged requests | "We retain inputs and outputs for up to 2 years" and "trust and safety classification scores for up to 7 years" | D2 |
| Feedback | "we retain data associated with that submission for 5 years" | D2 |
| Default (docs) | "Conversation content (your prompts and Claude's outputs) is not retained by default; the exception is Covered Models, which require 30-day retention." | D3 |
| Regardless of arrangement | "if a chat or session is flagged, Anthropic may retain inputs and outputs for up to 2 years." | D3 |
| Zero data retention | "Under a ZDR arrangement, Anthropic does not store customer prompts or responses at rest after the API response is returned. To request ZDR for your organization, contact the Anthropic sales team." | D3 |
| Structured outputs | "Your prompts and Claude's outputs are not stored. Only the JSON schema is cached, for up to 24 hours since last use." | D3 |
| Batch processing | "29-day retention; async storage required." | D3 |
| Covered Models (Claude Fable, Claude Mythos) | "These models require 30-day data retention and are not available under ZDR unless expressly authorized by Anthropic." | D3 |

#### Training

| Fact | Quote | Src |
|---|---|---|
| Commercial Terms, section B | "Anthropic may not train models on Customer Content from Services." | D4 |
| Docs | "Retained data is never used for model training without your express permission." | D3 |

#### Contract

| Fact | Quote | Src |
|---|---|---|
| Data processing addendum included | "Data submitted through the Services will be processed in accordance with the Anthropic Data Processing Addendum", "which is incorporated into these Terms by reference." (section C) | D4 |
| Roles | "With respect to Customer Personal Data, Customer is the controller and Anthropic is Customer's processor." | D5 |
| Transfers | "the terms of the SCCs Module Two (controller to processor) and/or Module Three (processor to processor)" are incorporated (section I.1). EU–US Data Privacy Framework: not stated | D5 |
| Processing locations | Not stated | D4, D5 |
| Subprocessors | "Anthropic's list of subprocessors is available at https://www.anthropic.com/subprocessors" (Schedule 4). The URL redirects to https://trust.anthropic.com/subprocessors, which can't be read automatically (only the page title came back) | D5 |
| Deletion after the contract | "Within thirty (30) days of the date of termination or expiration of the Agreement", Anthropic will "delete all copies of Customer Data (including Customer Personal Data)" (section H.1) | D5 |
| Outputs | The customer "(b) owns its Outputs." (section B) | D4 |

#### API keys and calls from the client

| Fact | Quote | Src |
|---|---|---|
| Recommendation | "Call the Claude API from your own back end, not from code that runs in your users' browsers. Anyone who uses a webpage can read an API key in that page's code. Typically, your web front end calls your back end, which holds the key and calls the Claude API." | D6 |
| Browser calls | "The API accepts these requests only when they include the `anthropic-dangerous-direct-browser-access: true` header" | D6 |
| Native apps | "App Attest authenticates iOS and macOS apps that call the Claude API directly from the device." "Tokens are scoped to your workspace, expire after 1 hour, and authorize only Messages API calls." An equivalent for Android or web apps: not stated | D6 |
| Key hygiene | "Store API keys in a secrets manager, rotate them periodically, and disable or delete any key you suspect has leaked." Expiration presets "3 hours, 1 day, 7 days, or 30 days". "To limit where a leaked key works, turn on IP allowlisting." | D6 |

- D1 https://platform.claude.com/docs/en/manage-claude/data-residency
- D2 https://privacy.claude.com/en/articles/7996866-how-long-do-you-store-my-organization-s-data
- D3 https://platform.claude.com/docs/en/manage-claude/api-and-data-retention
- D4 https://www.anthropic.com/legal/commercial-terms (effective date shown: Jun 17, 2025)
- D5 https://www.anthropic.com/legal/data-processing-addendum (effective date shown: Feb 24, 2025)
- D6 https://platform.claude.com/docs/en/manage-claude/authentication

### Contradictions in the official pages

1. **Default retention:** D2 says inputs and outputs are deleted "within 30 days of receipt or generation"; D3 says conversation content "is not retained by default". The docs assume the stricter reading: kept up to 30 days, up to 2 years if flagged.

### Gaps

- The subprocessor list (which cloud providers, which countries) can't be read automatically.
- Whether the EU–US Data Privacy Framework is used: not stated in D5.

### Observations for Parts B and E (not a decision)

- There is no EU option: inference runs in the US or "any available geography", storage is in the US. The transfer is covered by the data processing addendum and the Standard Contractual Clauses. The measure on our side is to send as little personal data as possible in the prompt.
- Question for topic 3: can Firebase AI Logic (Gemini) process data in the EU? That would be a difference between V2 and V3 on the privacy criterion.
- For V1, Anthropic's own docs advise against an API key in client code (D6).

## 3 · Firebase AI Logic (V3)

**Question.** What can the PDF's example for V3 (BaaS with managed AI) do, and what can't it? Models, place of processing, protection of the key, rate limits per user, control over prompt and answer, costs. Plus the open question from topic 1: can Firebase Authentication and Cloud Firestore be used from a backend hosted elsewhere? Needed for a fair V3 column in Part B.

All pages in this section were read on **2026-11-22**.

### Summary

| Question | Answer | Src |
|---|---|---|
| Models | Gemini only ("Most of the Gemini foundation models"); non-Google models such as Claude not mentioned | L1, L11 |
| Providers | Gemini Developer API (Spark or Blaze, global only) or Agent Platform, formerly Vertex AI (always Blaze, location selectable) | L3, L11 |
| Free tier for EU users | Not allowed: only Paid Services for apps serving users in the EEA, Switzerland or the UK | T1 |
| Place of processing | Developer API: global. Agent Platform: multi-region `eu` available; default `global` | L3, L11, X2 |
| Key in the app | No Gemini API key in the app; requests go through a Firebase proxy; App Check enforced automatically since 2026-11-02 | L6, L11 |
| Rate limit | Default 100 requests per minute per user, adjustable only for the whole project; what identifies a user: not stated | L7 |
| Prompt on the server | Server prompt templates (Preview) | L9 |
| Structured answer | JSON schema with enum supported; a server-side check before the answer reaches the app: not stated | L10 |
| Costs | Firebase AI Logic itself free; Gemini usage on paid tiers needs Blaze and, for most projects, a prepayment | L5 |
| Admin SDK | Runs outside Google Cloud with a service account key file; Python supported; Spark availability not stated | K1 |

### Details

#### What it is, models, providers

| Fact | Quote | Src |
|---|---|---|
| What it is | "Firebase AI Logic gives you access to the latest generative AI models from Google: the Gemini models." | L1 |
| Platforms | "Swift for Apple platforms, Kotlin & Java for Android, JavaScript for web, Dart for Flutter, and Unity." | L1 |
| Providers | "We support both the Gemini Developer API and Agent Platform Gemini API (formerly Vertex AI)." | L1 |
| Supported models | "Firebase AI Logic supports the following models:" "Most of the Gemini foundation models". Non-Google models (e.g. Claude): not stated | L11 |
| Not supported | "Fine-tuned models" and "Embedding models, Veo models, Lyria models, robotics models, computer use models, Gemini Omni models, or Gemini Cyber models" | L11 |
| Plans | Gemini Developer API: "Available on both the no-cost Spark pricing plan and the pay-as-you-go Blaze pricing plan"; Agent Platform: "Always requires the pay-as-you-go Blaze pricing plan (when used with Firebase AI Logic)" | L11 |
| Provider rate limits | Gemini Developer API: "Explicit rate limits"; Agent Platform: "Uses dynamic shared quota (DSQ) that everyone using that model in that region shares." | L11 |
| Location | The Gemini Developer API "provides only global access to models"; Agent Platform "provides both global access (recommended for most use cases) and setting a specific location" | L3 |
| Global pool | "The Gemini Developer API provides global access to models, which means your request will be handled by an available model anywhere in the global pool." | L4 |
| Default location | "the default location is `global`" (current SDK syntax) | L11 |

#### Gemini API terms: free and paid tier

The Gemini API Additional Terms are marked "Effective March 23, 2026". Firebase: "Your use of Firebase AI Logic is governed by the Google Cloud Platform Terms of Service." (L14); for the Gemini Developer API, the Gemini API terms apply in addition (L14).

| Fact | Quote | Src |
|---|---|---|
| Free tier ("Unpaid Services") | "Any Services that are offered free of charge like direct interactions with Google AI Studio or unpaid quota in Gemini API are unpaid Services" | T1 |
| Use of free-tier data | "Google uses the content you submit to the Services and any generated responses to provide, improve, and develop" | T1 |
| Human review | "human reviewers may read, annotate, and process your API input and output." | T1 |
| Warning | "Do not submit sensitive, confidential, or personal information to the Unpaid Services." | T1 |
| EU users | "You may use only Paid Services when making API Clients available to users in the European Economic Area, Switzerland, or the United Kingdom." | T1 |
| Paid tier | "Your access to Gemini API is a "Paid Service" only when accessing the API through a Cloud Project associated with an active billing account." | T1 |
| No product improvement on the paid tier | "Google doesn't use your prompts (including associated system instructions, cached content, and files such as images, videos, or documents) or responses to improve our products" | T1 |
| Abuse logging on the paid tier | "For Paid Services, Google logs prompts and responses for a limited period of time, solely for detecting and preventing violations of the Prohibited Use Policy" (duration not stated) | T1 |
| Location on the paid tier | "This data may be stored transiently or cached in any country in which Google or its agents maintain facilities." | T1 |
| Contract on the paid tier | Google "will process your prompts and responses in accordance with the Data Processing Addendum for Products Where Google is a Data Processor." | T1 |

#### Agent Platform (Vertex AI): EU processing and data use

| Fact | Quote | Src |
|---|---|---|
| Multi-region | "location must be set to 'us' or 'eu' for multi-region routing" | X2 |
| Purpose of multi-regions | "ensure that machine learning processing of Customer Data by the service stays within a specific jurisdictional boundary" | X2 |
| Caveat | "Important: Endpoints don't guarantee data residency or in-region ML processing." | X2 |
| Global endpoint | "Don't use the global endpoint if you have ML processing requirements, because you can't control or know which region your ML processing requests are sent to" | X2 |
| Gemini models in `eu` | Not readable (the availability cells of the table come through empty) | X2 |
| Training | "Google won't use your data to train or fine-tune any AI/ML models without your prior permission or instruction." | X1 |
| Abuse logging | "Google may log prompts to detect potential abuse and violations of its Acceptable Use Policy" (duration not stated on the page) | X1 |
| Caching | "By default, Google's published Gemini models cache Customer Data to reduce latency and accelerate responses." "It's deleted no later than 24 hours after it's written." Caching can be disabled per project | X1 |
| Cache location | "It stays within the selected location and adheres to its Data Residency requirements when you use a regional or multi-regional endpoint." "When you use the global endpoint, requests and their cached data can be served from any region." | X1 |

#### Key protection and abuse

| Fact | Quote | Src |
|---|---|---|
| No key in the app | "you should *not* add a Gemini API key into your app's codebase." "Firebase AI Logic no longer uses standard Gemini API keys for authorizing use of the Gemini Developer API." | L11 |
| Proxy | The Firebase AI Logic backend "acts as a proxy gateway where Firebase App Check verification takes place" | L6 |
| App Check | "When you enforce App Check, it will only allow incoming requests that are verified to be from your actual app and/or an untampered device." | L6 |
| Enforced automatically | "On November 2, 2026, Firebase automatically enforced App Check for Firebase AI Logic in all projects"; "Unenforcing App Check for Firebase AI Logic is *strongly NOT recommended*" | L6 |
| Rate limit | "The default rate limit is 100 RPM per user." "It's based on "Generate content requests" on a per-user per-region per-minute basis" | L7 |
| Not per person | "Currently, there isn't a way to set the rate limit for a specific user or specific group of users" | L7 |
| What identifies a user | Not stated | L7, L11, L13 |
| Authenticated-users mode | "When enforced, all Gemini API requests via Firebase AI Logic must come from *authenticated users* of your app." "This setting *cannot* be applied on a per-user, per-API, or per-template basis." Release stage: not stated | L12 |
| Weak sign-in methods | "Anonymous or Email/Password auth methods… are not robust forms of authentication (especially for protecting a paid resource)." | L12 |
| Production checklist | "Enforce Firebase App Check to help protect the Gemini API from abuse when it's called directly from your app"; "Set rate limits per user (default is 100 RPM) to prevent individual client instances from consuming excessive quota."; "Avoid surprise bills with alerts and spend caps" | L8 |

#### Control over prompt and answer

| Fact | Quote | Src |
|---|---|---|
| Server prompt templates | "you store your prompt, schema, and configurations server-side, and your app's codebase passes from the client to the server only a key (the template ID)" | L9 |
| Release stage | "Using server prompt templates is a feature that's in Preview, which means that it isn't subject to any SLA or deprecation policy" | L9 |
| Prompt injection | Templates "that accept dynamic variables can still be susceptible to prompt injection" | L13 |
| Client override of a template | Not stated (the page on template-only mode was not read) | L9, L13 |
| Structured output | "To ensure that the model's generated output always adheres to a specific schema"; `enum` is a supported schema field | L10 |
| Unsupported schema fields | "If you use an unsupported field, the model can still handle your request, but it ignores the field." | L10 |
| Server-side check of the answer | Not stated; every example reads the answer directly in the app | L10 |

#### Costs

| Fact | Quote | Src |
|---|---|---|
| Firebase AI Logic | "Using Firebase AI Logic is free-of-charge." | L5 |
| Free tier | "The Gemini Developer API "free tier" lets you get started without having to provide a payment method." | L5 |
| Paid tiers | "To use these "paid tiers", you must link your project to a Cloud Billing account." "the Gemini Developer API requires most projects to *provide a prepayment* to access the API" | L5 |

#### Firebase Admin SDK (open question from topic 1)

| Fact | Quote | Src |
|---|---|---|
| Outside Google Cloud | "If you are working in a non-Google server environment in which default credentials lookup can't be fully automated, you can initialize the SDK with an exported service account key file." | K1 |
| Python | "Python 3.10+ (recommend Python 3.11+)"; Cloud Firestore and Firebase Authentication are in the feature matrix for all languages | K1 |
| On the Spark plan | Not stated (K1 and F2 don't mention the Admin SDK and plans together) | K1, F2 |

- L1 https://firebase.google.com/docs/ai-logic
- L2 https://firebase.google.com/docs/ai-logic/get-started
- L3 https://firebase.google.com/docs/ai-logic/models
- L4 https://firebase.google.com/docs/ai-logic/locations
- L5 https://firebase.google.com/docs/ai-logic/pricing
- L6 https://firebase.google.com/docs/ai-logic/app-check
- L7 https://firebase.google.com/docs/ai-logic/quotas
- L8 https://firebase.google.com/docs/ai-logic/production-checklist
- L9 https://firebase.google.com/docs/ai-logic/server-prompt-templates/get-started
- L10 https://firebase.google.com/docs/ai-logic/generate-structured-output
- L11 https://firebase.google.com/docs/ai-logic/faq-and-troubleshooting
- L12 https://firebase.google.com/docs/ai-logic/auth-mode
- L13 https://firebase.google.com/docs/ai-logic/security-checklist
- L14 https://firebase.google.com/docs/ai-logic/data-governance
- T1 https://aistudio.google.com/docs/terms (Gemini API Additional Terms; https://ai.google.dev/gemini-api/terms redirects there)
- X1 https://docs.cloud.google.com/vertex-ai/generative-ai/docs/data-governance
- X2 https://docs.cloud.google.com/vertex-ai/generative-ai/docs/learn/locations
- K1 https://firebase.google.com/docs/admin/setup
- F2 see section 1

### Contradictions in the official pages

1. **Live API on Agent Platform:** L11 lists the Gemini Live API as supported by both providers; L3 marks a Live model as "*not supported*" on Agent Platform. Not relevant for this task.

### Gaps

- What identifies a "user" for the rate limit (Firebase Auth user, app instance or IP).
- Which Gemini models are available in the `eu` multi-region.
- Whether the app can override a template's prompt or settings.
- Whether the Admin SDK works on the Spark plan; if Part B chooses a Firebase-based stack, a short test in the project settles it.
- How long abuse logs are kept on the paid tier.

### Observations for Part B (not a decision)

- V3 means a model switch: Firebase AI Logic offers only Gemini, not Claude.
- V3 needs the paid plan: the free tier is not allowed for apps serving users in the EU (T1). So: Blaze, a card and a budget alert; Google's spend cap covers the Gemini API (section 1, G3).
- Strengths of V3: no key in the app, App Check enforced by default, and with Agent Platform processing can be kept in the `eu` multi-region; on the privacy criterion this is stronger than the Claude API, which has no EU option (section 2).
- Weaknesses of V3 against the Part C requirements: no documented server-side check of the answer, no rate limit per person (only per "user" with an unstated identifier, adjustable only project-wide), server prompt templates still in Preview. Meeting these would need own server code, which moves V3 towards V2.
- For a V2 backend that uses Firebase Authentication and Firestore from outside Google Cloud, the service account key file is a credential and belongs in the platform's secret store, never in the repo.
