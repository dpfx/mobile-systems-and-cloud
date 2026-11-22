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
