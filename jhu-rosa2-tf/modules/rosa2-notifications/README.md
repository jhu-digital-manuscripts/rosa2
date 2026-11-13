# Rosa notification module

The monitoring module calls this module from its `notifications.tf` file. It creates `jhu-rosa2-stage-alarms` and `jhu-rosa2-alarms` (prod)
in us-east-1 and associates them with the existing Amazon Q Slack connector.
Topic policies allow publication from this account's matching Rosa alarms.
Amazon Q creates the HTTPS subscriptions when the topics are associated.

The connector is shared across stage, prod, and other applications. To keep one
state owner, `environment = "stage"` creates both topics and manages the
connector. `environment = "prod"` only looks up `jhu-rosa2-alarms`; it creates no
SNS or connector resources. Both environments' image, search and functional
alarms use the module's `alarm_topic_arn` output.

Root `imports.tf` has a conditional import block that adopts the existing
`jhu-aws-slack-connector-staging` configuration into **stage only**. The separate
`aws.chatbot` provider accesses it through us-east-2 without inheriting stage
tags. Its existing four SNS routes, channel, IAM role, guardrails, logging,
user-authorization setting, and tags are preserved. The current destination is
workspace `TBYPTS56V`, channel `C071QCVBGJJ` (`devops-aws-alerts-drcc`).

## Deployment and ownership

There is no separate notifications backend, init, or apply. From `jhu-rosa2-tf`:

```sh
tofu init -reconfigure -backend-config=stage.s3.tfbackend
tofu plan -var-file=stage.tfvars -out=stage.tfplan
# Apply the reviewed plan only with explicit authorization.
tofu apply stage.tfplan
```

This single stage apply includes topics, connector adoption, subscriptions,
application alarms/checker, and other stage infrastructure changes. Prod's normal
plan/apply can follow once the topic exists. Prod never imports or updates the
shared connector, so applying prod cannot overwrite stage/other SNS routes.

Before applying the import, ensure the connector is not managed by another
Terraform/OpenTofu or CloudFormation state. If it is, transfer ownership without
destroying it first. The whole SNS topic list belongs to stage after adoption;
add future shared routes in this module so later stage applies retain them.
The connector has `prevent_destroy`, which also prevents destroying the whole
owning stage stack while it manages the shared connector. Transfer ownership
explicitly if stage must be retired while prod continues using notifications.

The previous standalone notifications proposal was never applied in this
session, so no state migration is needed. If it was applied independently,
transfer its resources into the stage module addresses before applying this
configuration; do not import one remote resource into two active states.

The topic names follow the repository's fixed stage/prod name prefixes. If those
change, update `local.applications` here too. After deployment verify the topics'
confirmed subscriptions and checker metrics. Test Slack notifications only when
coordinated with the channel owners.

## Offline validation

```sh
cd jhu-rosa2-tf/modules/rosa2-notifications
tofu init -backend=false
tofu test
```

Tests use mocked providers and plan only. They check topic policies, preservation
of the shared connector settings/routes, and that prod reads its topic without
owning any shared resources. Keeping the import in the root avoids importing a
real resource during module tests.

A root `moved` block preserves existing notification state when moving from
`module.rosa2_notifications` to
`module.rosa2_monitoring.module.rosa2_notifications`. No resource replacement
is required if the prior layout has already been applied.
