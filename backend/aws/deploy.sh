#!/bin/bash
# ============================================================
# Deployment script — builds the JAR, uploads to S3, and
# triggers a rolling update on the Auto Scaling Group.
#
# Usage:
#   ./deploy.sh [staging|production]
#
# Prerequisites:
#   - AWS CLI configured with appropriate IAM role
#   - Maven installed
#   - S3 bucket "adaptive-ticket-artifacts" exists
# ============================================================

set -euo pipefail

ENV=${1:-staging}
echo "==> Deploying to: $ENV"

# 1. Build the JAR
echo "==> Building application..."
cd "$(dirname "$0")/.."
mvn clean package -DskipTests -q
JAR_PATH="target/adaptive-ticket-pricing-1.0.0.jar"

if [ ! -f "$JAR_PATH" ]; then
  echo "ERROR: JAR not found at $JAR_PATH"
  exit 1
fi

# 2. Upload to S3
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
S3_KEY="releases/${ENV}/app-${TIMESTAMP}.jar"
echo "==> Uploading to s3://adaptive-ticket-artifacts/${S3_KEY}"
aws s3 cp "$JAR_PATH" "s3://adaptive-ticket-artifacts/${S3_KEY}"

# Also update the "latest" pointer
aws s3 cp "$JAR_PATH" "s3://adaptive-ticket-artifacts/app.jar"

# 3. Trigger rolling update (replaces instances one at a time)
ASG_NAME="adaptive-ticket-${ENV}-asg"
echo "==> Starting instance refresh on ${ASG_NAME}..."
aws autoscaling start-instance-refresh \
  --auto-scaling-group-name "$ASG_NAME" \
  --preferences '{"MinHealthyPercentage": 50, "InstanceWarmup": 120}'

echo "==> Deployment initiated. Monitor at:"
echo "    https://console.aws.amazon.com/ec2/autoscaling/home"
echo ""
echo "==> To check status:"
echo "    aws autoscaling describe-instance-refreshes --auto-scaling-group-name $ASG_NAME"
